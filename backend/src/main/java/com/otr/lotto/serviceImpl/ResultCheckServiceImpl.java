package com.otr.lotto.serviceImpl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
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

@Service
@RequiredArgsConstructor
public class ResultCheckServiceImpl implements ResultCheckService {
    private static final long DEFAULT_EVENT_ID = 1L;

    private final EventMapper eventMapper;
    private final ParticipantMapper participantMapper;
    private final PrizeMapper prizeMapper;

    @Transactional
    @Override
    public ResultCheckResponse check(ResultCheckRequest request) {
        Event event = eventMapper.findById(DEFAULT_EVENT_ID);
        if (event == null) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }

        validateAnnouncePeriod(event);

        String phoneHash = hashPhone(request.getPhone());
        Participant participant = participantMapper.findByEventAndPhoneHash(event.getId(), phoneHash);
        if (participant == null) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }

        int currentCount = participant.getCheckCount() != null ? participant.getCheckCount() : 0;
        Prize prize = prizeMapper.findByEventAndParticipantId(event.getId(), participant.getId());
        boolean isWinner = prize != null;

        LocalDateTime checkedAt = LocalDateTime.now();
        participantMapper.updateCheckCountAndTimestamps(event.getId(), participant.getId(), checkedAt);

        if (currentCount == 0) {
            Integer rank = prize != null ? prize.getRank() : null;
            return new ResultCheckResponse(rank, null, null, null);
        }

        return new ResultCheckResponse(null, isWinner, null, null);
    }

    private void validateAnnouncePeriod(Event event) {
        LocalDate today = LocalDate.now();
        if (today.isBefore(event.getAnnounceStart()) || today.isAfter(event.getAnnounceEnd())) {
            throw new ApiException(ErrorCode.ANNOUNCE_NOT_ACTIVE);
        }
    }

    private String hashPhone(String phone) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(phone.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
