package com.otr.lotto.serviceImpl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.otr.lotto.common.ApiException;
import com.otr.lotto.common.ErrorCode;
import com.otr.lotto.domain.Event;
import com.otr.lotto.domain.Participant;
import com.otr.lotto.domain.SmsLog;
import com.otr.lotto.domain.TicketPool;
import com.otr.lotto.dto.ParticipateRequest;
import com.otr.lotto.dto.ParticipateResponse;
import com.otr.lotto.mapper.EventMapper;
import com.otr.lotto.mapper.ParticipantMapper;
import com.otr.lotto.mapper.SmsLogMapper;
import com.otr.lotto.mapper.TicketPoolMapper;
import com.otr.lotto.service.ParticipationService;

import lombok.RequiredArgsConstructor;

/**
 * 로또 이벤트 참여 서비스
 * 
 * 참여자의 휴대폰 번호 등록, 중복 검사, 사전 생성된 번호 풀에서
 * 확정 번호 배정, 문자 발송 이력 기록을 담당합니다.
 * 
 * 주요 책임:
 * - 중복 참여 방지 (휴대폰 번호 정규화 + SHA256 해싱)
 * - 참여자에게 번호 풀 seq 기반 번호 배정
 * - 지정 휴대폰 1등 보장 (번호 스왑 로직)
 * - SMS 발송 이력 기록
 */
@Service
@RequiredArgsConstructor
public class ParticipationServiceImpl implements ParticipationService {
    private static final int DEFAULT_MAX_PARTICIPANTS = 10_000;
    private static final String SMS_TYPE_PARTICIPATION_NUMBER = "PARTICIPATION_NUMBER";
    private static final String SMS_STATUS_SENT = "SENT";

    private final EventMapper eventMapper;
    private final ParticipantMapper participantMapper;
    private final SmsLogMapper smsLogMapper;
    private final TicketPoolMapper ticketPoolMapper;
    private final com.otr.lotto.common.CurrentDateProvider currentDateProvider;

    /**
     * 로또 이벤트 참여 처리
     * 
     * 1. 현재 활성 이벤트 확인
     * 2. 정원 여부 확인
     * 3. 중복 참여 여부 검사 (휴대폰 번호 기반)
     * 4. 참여자 정보 등록 (ID는 자동증가 = seq)
     * 5. 번호 풀 seq 기반 번호 배정
     * 6. 번호와 참여자 매핑 기록
     * 7. SMS 발송 이력 저장
     * 
     * @param request 휴대폰 번호를 포함한 참여 요청
     * @return 참여순번과 배정된 로또 번호
     * @throws ApiException 이벤트 미활성/정원 만석/중복 참여 시
     */
    @Transactional
    @Override
    public ParticipateResponse participate(ParticipateRequest request) {
        // 현재 활성화된 이벤트 자동 조회
        Event event = eventMapper.findActiveEvent(currentDateProvider.today());
        if (event == null) {
            throw new ApiException(ErrorCode.EVENT_NOT_ACTIVE);
        }
        validateCapacity(event);

        String phoneHash = hashPhone(request.getPhone());
        Participant existing = participantMapper.findByEventAndPhoneHash(event.getId(), phoneHash);
        if (existing != null) {
            throw new ApiException(ErrorCode.DUPLICATE_PARTICIPATION);
        }

        Participant participant = new Participant();
        participant.setEventId(event.getId());
        participant.setPhoneHash(phoneHash);

        try {
            participantMapper.insert(participant);
        } catch (DuplicateKeyException ex) {
            throw new ApiException(ErrorCode.DUPLICATE_PARTICIPATION);
        }

        if (participant.getId() == null) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }

        String lottoNumber = assignLottoNumber(event, participant, phoneHash);

        SmsLog smsLog = new SmsLog();
        smsLog.setEventId(event.getId());
        smsLog.setParticipantId(participant.getId());
        smsLog.setPhoneHash(phoneHash);
        smsLog.setType(SMS_TYPE_PARTICIPATION_NUMBER);
        smsLog.setSentDate(currentDateProvider.today());
        smsLog.setStatus(SMS_STATUS_SENT);
        smsLogMapper.insert(smsLog);

        return new ParticipateResponse(participant.getId(), lottoNumber);
    }



    /**
     * 이벤트 정원 여부 확인
     * 
     * @param event 이벤트 정보
     * @throws ApiException 정원이 가득 찬 경우
     */
    private void validateCapacity(Event event) {
        long currentCount = participantMapper.countByEvent(event.getId());
        Integer maxParticipants = event.getMaxParticipants();
        int maxParticipantsValue = maxParticipants == null
            ? DEFAULT_MAX_PARTICIPANTS
            : maxParticipants;

        if (currentCount >= maxParticipantsValue) {
            throw new ApiException(ErrorCode.CAPACITY_FULL);
        }
    }

    /**
     * 참여자에게 번호 배정
     * 
     * 참여순번(participant.id)을 기준으로 사전 생성된 번호 풀에서
     * 해당 seq 데이터를 검색하여 배정합니다.
     * 
     * 지정 휴대폰 1등 보장 처리:
     * - 지정 번호 참여 시 → 미배정 1등 번호 찾아서 현재 번호와 스왑
     * - 지정 번호 아닌데 1등 seq 배정 시 → 비당첨 번호와 스왑
     * 
     * @param event 이벤트 정보
     * @param participant 참여자 정보
     * @param phoneHash 정규화된 휴대폰 번호 해시
     * @return 배정된 로또 번호 (CSV 형태: 3,11,22,33,41,45)
     * @throws ApiException 번호 풀 미준비 또는 배정 실패 시
     */
    private String assignLottoNumber(Event event, Participant participant, String phoneHash) {
        TicketPool pool = ticketPoolMapper.findByEventAndSeq(event.getId(), participant.getId());
        if (pool == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "번호 풀이 준비되지 않았습니다.");
        }

        if (isFirstPrizePhone(event, phoneHash)) {
            pool = ensureFirstPrizeForParticipant(event.getId(), pool);
        } else if (isRankOne(pool)) {
            pool = swapWithNonWinner(event.getId(), pool);
        }

        int updated = ticketPoolMapper.assignParticipant(pool.getId(), participant.getId());
        if (updated != 1) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "번호 배정에 실패했습니다.");
        }

        return pool.getLottoNumber();
    }

    /**
     * 지정된 1등 보장 휴대폰 번호 확인
     * 
     * event.fixed_first_phone_hash와 비교하여
     * 현 참여자가 확정 1등 대상인지 판단합니다.
     * 
     * @param event 이벤트 정보
     * @param phoneHash 현 참여자 휴대폰 해시
     * @return 1등 보장 대상 여부
     */
    private boolean isFirstPrizePhone(Event event, String phoneHash) {
        String fixedFirstPhoneHash = event.getFixedFirstPhoneHash();
        if (fixedFirstPhoneHash == null || fixedFirstPhoneHash.trim().isEmpty()) {
            return false;
        }

        return fixedFirstPhoneHash.equals(phoneHash);
    }

    /**
     * 지정 휴대폰에게 1등 번호 보장
     * 
     * 현재 seq 번호가 1등이 아닌 경우,
     * 미배정 1등 번호를 찾아 현재 번호와 스왑합니다.
     * 이를 통해 지정 휴대폰은 항상 1등 번호를 받습니다.
     * 
     * @param eventId 이벤트 ID
     * @param seqPool 현재 seq의 번호 풀 정보
     * @return 1등 번호로 변경된 풀 정보
     * @throws ApiException 1등 번호가 모두 배정된 경우
     */
    private TicketPool ensureFirstPrizeForParticipant(Long eventId, TicketPool seqPool) {
        if (isRankOne(seqPool)) {
            return seqPool;
        }

        TicketPool firstPrizePool = ticketPoolMapper.findUnassignedByRank(eventId, 1);
        if (firstPrizePool == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "1등 번호가 이미 배정되었습니다.");
        }

        swapPoolEntries(seqPool, firstPrizePool);
        return seqPool;
    }

    /**
     * 1등 번호를 비당첨으로 변경
     * 
     * 1등 seq를 받은 일반 참여자의 경우,
     * 미배정 비당첨 번호와 스왑하여 1등 자리를 유지합니다.
     * 이는 지정 휴대폰에게 1등을 보장하기 위한 기전입니다.
     * 
     * @param eventId 이벤트 ID
     * @param seqPool 1등 seq의 번호 풀 정보
     * @return 비당첨 번호로 변경된 풀 정보
     * @throws ApiException 비당첨 번호가 없는 경우
     */
    private TicketPool swapWithNonWinner(Long eventId, TicketPool seqPool) {
        TicketPool nonWinner = ticketPoolMapper.findUnassignedByRank(eventId, 0);
        if (nonWinner == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "번호 배정 가능 데이터가 없습니다.");
        }

        swapPoolEntries(seqPool, nonWinner);
        return seqPool;
    }

    /**
     * 번호 풀 항목 스왑
     * 
     * 두 ticket_pool 항목의 rank와 lotto_number를 서로 교환합니다.
     * 데이터베이스와 메모리의 객체 상태를 모두 업데이트합니다.
     * 
     * @param left 변경할 풀 항목 1
     * @param right 변경할 풀 항목 2
     */
    private void swapPoolEntries(TicketPool left, TicketPool right) {
        int leftRank = normalizeRank(left.getRank());
        int rightRank = normalizeRank(right.getRank());
        String leftNumber = left.getLottoNumber();
        String rightNumber = right.getLottoNumber();

        ticketPoolMapper.updateRankAndNumber(left.getId(), rightRank, rightNumber);
        ticketPoolMapper.updateRankAndNumber(right.getId(), leftRank, leftNumber);

        left.setRank(rightRank);
        left.setLottoNumber(rightNumber);
        right.setRank(leftRank);
        right.setLottoNumber(leftNumber);
    }

    private boolean isRankOne(TicketPool pool) {
        return Objects.equals(normalizeRank(pool.getRank()), 1);
    }

    /**
     * rank 값 정규화
     * 
     * null 값을 0(비당첨)으로 변환하여
     * 안전한 rank 비교를 보장합니다.
     * 
     * @param rank rank 값 (1~4 또는 null/0)
     * @return 정규화된 rank
     */
    private int normalizeRank(Integer rank) {
        return rank == null ? 0 : rank;
    }

    /**
     * 휴대폰 번호 해싱
     * 
     * 휴대폰 번호를 정규화(숫자만 추출)한 후
     * SHA256으로 해싱합니다.
     * 
     * 용도:
     * - 중복 참여 방지
     * - 1등 고정 번호 매칭
     * 
     * @param phone 원본 휴대폰 번호 (형식 무관)
     * @return SHA256 해시값 (16진수 문자열)
     */
    private String hashPhone(String phone) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String normalized = normalizePhone(phone);
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * 휴대폰 번호 정규화
     * 
     * 숫자만 추출하여 형식 차이 제거
     * (예: 010-1234-5678 → 01012345678)
     * 
     * @param phone 원본 휴대폰 번호
     * @return 정규화된 번호 (숫자만)
     */
    private String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("\\D", "");
    }

    /**
     * 바이트 배열을 16진수 문자열로 변환
     * 
     * @param bytes 바이트 배열
     * @return 16진수 문자열
     */
    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
