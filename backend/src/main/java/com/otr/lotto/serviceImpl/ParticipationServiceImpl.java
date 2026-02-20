package com.otr.lotto.serviceImpl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Objects;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.otr.lotto.common.ApiException;
import com.otr.lotto.common.ErrorCode;
import com.otr.lotto.domain.Event;
import com.otr.lotto.domain.Participant;
import com.otr.lotto.domain.SmsLog;
import com.otr.lotto.domain.Ticket;
import com.otr.lotto.domain.TicketPool;
import com.otr.lotto.dto.ParticipateRequest;
import com.otr.lotto.dto.ParticipateResponse;
import com.otr.lotto.mapper.EventMapper;
import com.otr.lotto.mapper.ParticipantMapper;
import com.otr.lotto.mapper.SmsLogMapper;
import com.otr.lotto.mapper.TicketMapper;
import com.otr.lotto.mapper.TicketPoolMapper;
import com.otr.lotto.service.ParticipationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParticipationServiceImpl implements ParticipationService {
    private static final int DEFAULT_MAX_PARTICIPANTS = 10_000;
    private static final String SMS_TYPE_PARTICIPATION_NUMBER = "PARTICIPATION_NUMBER";
    private static final String SMS_STATUS_SENT = "SENT";

    private final EventMapper eventMapper;
    private final ParticipantMapper participantMapper;
    private final TicketMapper ticketMapper;
    private final SmsLogMapper smsLogMapper;
    private final TicketPoolMapper ticketPoolMapper;

    @Transactional
    @Override
    public ParticipateResponse participate(ParticipateRequest request) {
        // 현재 활성화된 이벤트 자동 조회
        Event event = eventMapper.findActiveEvent(LocalDate.now());
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
        Ticket ticket = new Ticket();
        ticket.setEventId(event.getId());
        ticket.setParticipantId(participant.getId());
        ticket.setLottoNumber(lottoNumber);
        ticketMapper.insert(ticket);

        SmsLog smsLog = new SmsLog();
        smsLog.setEventId(event.getId());
        smsLog.setParticipantId(participant.getId());
        smsLog.setPhoneHash(phoneHash);
        smsLog.setType(SMS_TYPE_PARTICIPATION_NUMBER);
        smsLog.setSentDate(LocalDate.now());
        smsLog.setStatus(SMS_STATUS_SENT);
        smsLogMapper.insert(smsLog);

        return new ParticipateResponse(participant.getId(), lottoNumber);
    }



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

    private boolean isFirstPrizePhone(Event event, String phoneHash) {
        String fixedFirstPhoneHash = event.getFixedFirstPhoneHash();
        if (fixedFirstPhoneHash == null || fixedFirstPhoneHash.trim().isEmpty()) {
            return false;
        }

        return fixedFirstPhoneHash.equals(phoneHash);
    }

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

    private TicketPool swapWithNonWinner(Long eventId, TicketPool seqPool) {
        TicketPool nonWinner = ticketPoolMapper.findUnassignedByRank(eventId, 0);
        if (nonWinner == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "번호 배정 가능 데이터가 없습니다.");
        }

        swapPoolEntries(seqPool, nonWinner);
        return seqPool;
    }

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

    private int normalizeRank(Integer rank) {
        return rank == null ? 0 : rank;
    }

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
