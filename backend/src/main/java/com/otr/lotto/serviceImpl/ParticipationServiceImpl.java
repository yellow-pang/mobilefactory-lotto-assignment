package com.otr.lotto.serviceImpl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        List<Integer> winningNumbers = getWinningNumbers(event);
        String winningNumbersCsv = formatNumbers(winningNumbers);

        if (isFirstPrizePhone(event, phoneHash)) {
            return winningNumbersCsv;
        }

        long participantId = participant.getId();
        long eventId = event.getId();

        if (isWithinRange(participantId, 2000, 7000)
                && countMatchInRange(eventId, 2000, 7000, winningNumbers, 5) < 5) {
            return generateVariantNumbers(winningNumbers, 5);
        }

        if (isWithinRange(participantId, 1000, 8000)
                && countMatchInRange(eventId, 1000, 8000, winningNumbers, 4) < 44) {
            return generateVariantNumbers(winningNumbers, 4);
        }

        if (countMatchInEvent(eventId, winningNumbers, 3) < 950) {
            return generateVariantNumbers(winningNumbers, 3);
        }

        return generateNonWinningNumbers(winningNumbers, 2);
    }

    private boolean isFirstPrizePhone(Event event, String phoneHash) {
        String fixedFirstPhoneHash = event.getFixedFirstPhoneHash();
        if (fixedFirstPhoneHash == null || fixedFirstPhoneHash.trim().isEmpty()) {
            return false;
        }

        return fixedFirstPhoneHash.equals(phoneHash);
    }

    private List<Integer> getWinningNumbers(Event event) {
        String winningNumberValue = event.getWinningNumber();
        if (winningNumberValue == null || winningNumberValue.trim().isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "이벤트 당첨 번호가 설정되지 않았습니다.");
        }

        List<Integer> numbers = parseNumbers(winningNumberValue);
        if (numbers.size() != 6) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "이벤트 당첨 번호는 6개의 숫자여야 합니다.");
        }
        return numbers;
    }

    private boolean isWithinRange(long value, long start, long end) {
        return value >= start && value <= end;
    }

    private int countMatchInRange(
        long eventId,
        long startId,
        long endId,
        List<Integer> winningNumbers,
        int targetMatch
    ) {
        List<Ticket> tickets = ticketMapper.findByParticipantIdRange(eventId, startId, endId);
        return countMatchingTickets(tickets, winningNumbers, targetMatch);
    }

    private int countMatchInEvent(long eventId, List<Integer> winningNumbers, int targetMatch) {
        List<Ticket> tickets = ticketMapper.findByEventId(eventId);
        return countMatchingTickets(tickets, winningNumbers, targetMatch);
    }

    private int countMatchingTickets(List<Ticket> tickets, List<Integer> winningNumbers, int targetMatch) {
        int count = 0;
        for (Ticket ticket : tickets) {
            int matchCount = countMatchingNumbers(winningNumbers, ticket.getLottoNumber());
            if (matchCount == targetMatch) {
                count++;
            }
        }
        return count;
    }

    private int countMatchingNumbers(List<Integer> winningNumbers, String lottoNumbersCsv) {
        Set<Integer> winningSet = new HashSet<>(winningNumbers);
        List<Integer> lottoNumbers = parseNumbers(lottoNumbersCsv);
        int matches = 0;
        for (int number : lottoNumbers) {
            if (winningSet.contains(number)) {
                matches++;
            }
        }
        return matches;
    }

    private String generateVariantNumbers(List<Integer> winningNumbers, int matchCount) {
        if (matchCount == 6) {
            return formatNumbers(winningNumbers);
        }

        List<Integer> winningPool = new ArrayList<>(winningNumbers);
        Collections.shuffle(winningPool);
        List<Integer> matches = new ArrayList<>(winningPool.subList(0, matchCount));

        List<Integer> nonWinningPool = new ArrayList<>();
        for (int number = 1; number <= 45; number++) {
            if (!winningNumbers.contains(number)) {
                nonWinningPool.add(number);
            }
        }

        Collections.shuffle(nonWinningPool);
        List<Integer> others = new ArrayList<>(nonWinningPool.subList(0, 6 - matchCount));

        List<Integer> result = new ArrayList<>(6);
        result.addAll(matches);
        result.addAll(others);
        Collections.sort(result);
        return formatNumbers(result);
    }

    private String generateNonWinningNumbers(List<Integer> winningNumbers, int maxMatch) {
        for (int attempt = 0; attempt < 1000; attempt++) {
            List<Integer> numbers = generateRandomNumbers();
            if (countMatchingNumbers(winningNumbers, formatNumbers(numbers)) <= maxMatch) {
                return formatNumbers(numbers);
            }
        }

        throw new ApiException(ErrorCode.INTERNAL_ERROR);
    }

    private List<Integer> generateRandomNumbers() {
        Set<Integer> numbers = new HashSet<>();
        while (numbers.size() < 6) {
            numbers.add(ThreadLocalRandom.current().nextInt(1, 46));
        }
        List<Integer> result = new ArrayList<>(numbers);
        Collections.sort(result);
        return result;
    }

    private List<Integer> parseNumbers(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String[] parts = value.split(",");
        List<Integer> numbers = new ArrayList<>(parts.length);
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                numbers.add(Integer.valueOf(trimmed));
            }
        }
        return numbers;
    }

    private String formatNumbers(List<Integer> numbers) {
        List<Integer> sorted = new ArrayList<>(numbers);
        Collections.sort(sorted);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(sorted.get(i));
        }
        return builder.toString();
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
