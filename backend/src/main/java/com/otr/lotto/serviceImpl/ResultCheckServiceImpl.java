package com.otr.lotto.serviceImpl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.otr.lotto.common.ApiException;
import com.otr.lotto.common.ErrorCode;
import com.otr.lotto.domain.Event;
import com.otr.lotto.domain.Participant;
import com.otr.lotto.domain.Prize;
import com.otr.lotto.dto.ResultCheckRequest;
import com.otr.lotto.dto.ResultCheckResponse;
import com.otr.lotto.mapper.EventMapper;
import com.otr.lotto.mapper.ParticipantMapper;
import com.otr.lotto.mapper.PrizeMapper;
import com.otr.lotto.service.ResultCheckService;

import lombok.RequiredArgsConstructor;

/**
 * 로또 당첨 결과 조회 서비스
 * 
 * 참여자의 휴대폰 번호 기반으로 당첨 여부와 결과를 조회합니다.
 * 
 * 특징:
 * - 활성 발표 기간(announceStart ~ announceEnd)에만 조회 가능
 * - 첫 조회: rank 반환 (1~4 또는 null)
 * - 재조회: isWinner 반환 (true/false)
 * - 조회 횟수와 시간 기록
 * - 휴대폰 번호는 정규화(숫자만) 후 SHA256 해싱
 */
@Service
@RequiredArgsConstructor
public class ResultCheckServiceImpl implements ResultCheckService {

    private final EventMapper eventMapper;
    private final ParticipantMapper participantMapper;
    private final PrizeMapper prizeMapper;
    private final com.otr.lotto.common.CurrentDateProvider currentDateProvider;

    /**
     * 당첨 결과 조회
     * 
     * 발표 기간 내에 당첨 여부를 조회합니다.
     * 조회 횟수에 따라 다른 정보 반환:
     * 
     * 1회차: rank(1~4 또는 null)
     * 2회차 이후: isWinner(true/false)
     * 
     * @param request 휴대폰 번호를 포함한 조회 요청
     * @return 조회 결과 (rank 또는 isWinner, 조회 횟수)
     * @throws ApiException 발표 기간 미활성, 참여자 미조회, 기타 오류
     */
    @Transactional
    @Override
    public ResultCheckResponse check(ResultCheckRequest request) {
        // 현재 발표 기간에 해당하는 이벤트 자동 조회
        Event event = eventMapper.findActiveAnnounceEvent(currentDateProvider.today());
        if (event == null) {
            throw new ApiException(ErrorCode.ANNOUNCE_NOT_ACTIVE);
        }

        String phoneHash = hashPhone(request.getPhone());
        Participant participant = participantMapper.findByEventAndPhoneHash(event.getId(), phoneHash);
        if (participant == null) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }

        Integer storedCount = participant.getCheckCount();
        int currentCount = storedCount == null ? 0 : storedCount;
        Prize prize = prizeMapper.findByEventAndParticipantId(event.getId(), participant.getId());
        boolean isWinner = prize != null;

        LocalDateTime checkedAt = LocalDateTime.now();
        participantMapper.updateCheckCountAndTimestamps(event.getId(), participant.getId(), checkedAt);

        ResultCheckResponse response = new ResultCheckResponse();
        response.setCheckCount(currentCount + 1);

        if (currentCount == 0) {
            Integer rank = prize != null ? prize.getRank() : null;
            response.setRank(rank);
        } else {
            response.setIsWinner(isWinner);
        }

        return response;
    }



    /**
     * 휴대폰 번호 해싱
     * 
     * 휴대폰 번호를 정규화(숫자만) 후 SHA256으로 해시합니다.
     * 
     * @param phone 원본 휴대폰 번호
     * @return SHA256 해시값 (16진수 문자열)
     */
    private String hashPhone(String phone) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
    /**
     * 휴대폰 번호 정규화
     * 
     * 숫자만 추출하여 형식 차이 제거
     * (예: 010-1234-5678 → 01012345678)
     * 
     * @param phone 원본 휴대폰 번호
     * @return 정규화된 번호 (숫자만)
     */
            String normalized = normalizePhone(phone);
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }
    /**
     * 바이트 배열을 16진수 문자열로 변환
     * 
     * @param bytes 바이트 배열
     * @return 16진수 문자열
     */
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("\\D", "");
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
