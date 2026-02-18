package com.otr.lotto.serviceImpl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.otr.lotto.common.ApiException;
import com.otr.lotto.common.ErrorCode;
import com.otr.lotto.domain.Event;
import com.otr.lotto.domain.Participant;
import com.otr.lotto.domain.SmsLog;
import com.otr.lotto.domain.Ticket;
import com.otr.lotto.dto.ParticipateRequest;
import com.otr.lotto.dto.ParticipateResponse;
import com.otr.lotto.mapper.EventMapper;
import com.otr.lotto.mapper.ParticipantMapper;
import com.otr.lotto.mapper.SmsLogMapper;
import com.otr.lotto.mapper.TicketMapper;
import com.otr.lotto.service.ParticipationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParticipationServiceImpl implements ParticipationService {
    private static final long DEFAULT_EVENT_ID = 1L;
    private static final int DEFAULT_MAX_PARTICIPANTS = 10_000;
    private static final String SMS_TYPE_PARTICIPATION_NUMBER = "PARTICIPATION_NUMBER";
    private static final String SMS_STATUS_SENT = "SENT";

    private final EventMapper eventMapper;
    private final ParticipantMapper participantMapper;
    private final TicketMapper ticketMapper;
    private final SmsLogMapper smsLogMapper;

    @Transactional
    @Override
    public ParticipateResponse participate(ParticipateRequest request) {
        Event event = eventMapper.findById(DEFAULT_EVENT_ID);
        if (event == null) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }

        validateEventPeriod(event);
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

        String lottoNumber = generateLottoNumber();
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

    private void validateEventPeriod(Event event) {
        LocalDate today = LocalDate.now();
        if (today.isBefore(event.getEventStart()) || today.isAfter(event.getEventEnd())) {
            throw new ApiException(ErrorCode.EVENT_NOT_ACTIVE);
        }
    }

    private void validateCapacity(Event event) {
        long currentCount = participantMapper.countByEvent(event.getId());
        int maxParticipants = event.getMaxParticipants() != null
                ? event.getMaxParticipants()
                : DEFAULT_MAX_PARTICIPANTS;

        if (currentCount >= maxParticipants) {
            throw new ApiException(ErrorCode.CAPACITY_FULL);
        }
    }

    private String generateLottoNumber() {
        int value = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        return String.format("%06d", value);
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
