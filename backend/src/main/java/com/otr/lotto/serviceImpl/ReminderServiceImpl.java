package com.otr.lotto.serviceImpl;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.otr.lotto.common.ApiException;
import com.otr.lotto.common.ErrorCode;
import com.otr.lotto.domain.Event;
import com.otr.lotto.domain.Participant;
import com.otr.lotto.domain.Prize;
import com.otr.lotto.domain.SmsLog;
import com.otr.lotto.dto.ReminderResponse;
import com.otr.lotto.mapper.EventMapper;
import com.otr.lotto.mapper.ParticipantMapper;
import com.otr.lotto.mapper.PrizeMapper;
import com.otr.lotto.mapper.SmsLogMapper;
import com.otr.lotto.service.ReminderService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReminderServiceImpl implements ReminderService {

    private final EventMapper eventMapper;
    private final PrizeMapper prizeMapper;
    private final ParticipantMapper participantMapper;
    private final SmsLogMapper smsLogMapper;

    @Override
    @Transactional
    public ReminderResponse sendUnconfirmedWinnerReminders(Long eventId) {
        // 1. Event 조회
        Event event = eventMapper.findById(eventId);
        if (event == null) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }

        // 2. 발표일로부터 10일 경과 확인
        LocalDate announceStart = event.getAnnounceStart();
        LocalDate targetDate = announceStart.plusDays(10);
        LocalDate today = LocalDate.now();

        if (today.isBefore(targetDate)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        // 3. 미확인 당첨자 조회 (check_count = 0)
        List<Prize> unconfirmedWinners = prizeMapper.findUnconfirmedWinners(eventId);

        // 4. 중복 발송 방지: 오늘 이미 발송된 대상자 확인
        List<SmsLog> existingReminders = smsLogMapper.findExistingReminders(
            eventId, 
            today, 
            "UNCONFIRMED_WINNER_REMINDER"
        );

        Set<Long> alreadySentParticipantIds = new HashSet<>();
        for (SmsLog log : existingReminders) {
            alreadySentParticipantIds.add(log.getParticipantId());
        }

        // 5. SMS 발송 대상 필터링 및 발송
        int sentCount = 0;
        int failedCount = 0;

        for (Prize prize : unconfirmedWinners) {
            Long participantId = prize.getParticipantId();

            // 이미 오늘 발송된 경우 스킵
            if (alreadySentParticipantIds.contains(participantId)) {
                continue;
            }

            // Participant 정보 조회 (phone_hash 필요)
            Participant participant = participantMapper.findById(participantId);

            if (participant == null) {
                failedCount++;
                continue;
            }

            // SMS 발송 (실제로는 모킹)
            SmsLog smsLog = new SmsLog();
            smsLog.setEventId(eventId);
            smsLog.setParticipantId(participantId);
            smsLog.setPhoneHash(participant.getPhoneHash());
            smsLog.setType("UNCONFIRMED_WINNER_REMINDER");
            smsLog.setSentDate(today);
            smsLog.setStatus("SENT");

            smsLogMapper.insert(smsLog);
            sentCount++;
        }

        // 6. 결과 반환
        ReminderResponse response = new ReminderResponse();
        response.setEventId(eventId);
        response.setTargetDate(targetDate);
        response.setTotalUnconfirmedWinners(unconfirmedWinners.size());
        response.setRemindersSent(sentCount);
        response.setRemindersFailed(failedCount);

        return response;
    }
}
