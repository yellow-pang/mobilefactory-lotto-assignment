package com.otr.lotto.serviceImpl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.otr.lotto.common.ApiException;
import com.otr.lotto.common.ErrorCode;
import com.otr.lotto.domain.Event;
import com.otr.lotto.domain.Prize;
import com.otr.lotto.domain.TicketPool;
import com.otr.lotto.dto.DrawResponse;
import com.otr.lotto.mapper.EventMapper;
import com.otr.lotto.mapper.PrizeMapper;
import com.otr.lotto.mapper.TicketPoolMapper;
import com.otr.lotto.service.DrawService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DrawServiceImpl implements DrawService {

    private final EventMapper eventMapper;
    private final PrizeMapper prizeMapper;
    private final TicketPoolMapper ticketPoolMapper;

    @Override
    @Transactional
    public DrawResponse executeDraw(Long eventId) {
        long existingPrizeCount = prizeMapper.countByEvent(eventId);
        if (existingPrizeCount >= 1000) {
            return buildDrawResponse(eventId);
        }

        Event event = eventMapper.findById(eventId);
        if (event == null) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }

        List<TicketPool> assignedWinners = ticketPoolMapper.findAssignedWinners(eventId);
        if (assignedWinners.size() < 1000) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "당첨 대상자가 부족합니다.");
        }

        List<Prize> prizes = new ArrayList<>(assignedWinners.size());
        for (TicketPool pool : assignedWinners) {
            Prize prize = new Prize();
            prize.setEventId(eventId);
            prize.setParticipantId(pool.getAssignedParticipantId());
            prize.setRank(pool.getRank());
            prizes.add(prize);
        }

        if (!prizes.isEmpty()) {
            prizeMapper.insertBatch(prizes);
        }

        return buildDrawResponse(eventId);
    }

    private DrawResponse buildDrawResponse(Long eventId) {
        DrawResponse response = new DrawResponse();
        response.setEventId(eventId);
        response.setTotalWinners(1000);
        response.setFirstPrizeCount(1);
        response.setSecondPrizeCount(5);
        response.setThirdPrizeCount(44);
        response.setFourthPrizeCount(950);
        return response;
    }
}
