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

/**
 * 로또 당첨 산정 서비스
 * 
 * 당첨 산정을 실행하는 서비스로, 사전 생성된 번호 풀에서
 * 당첨자(rank 정보 포함)를 읽어 Prize 레코드로 저장하는
 * 단순한 매핑 역할을 수행합니다.
 * 
 * 중요 특징: 번호 당첨 로직은 모두 사전에 ticket_pool에서 모두 처리됩니다.
 */
@Service
@RequiredArgsConstructor
public class DrawServiceImpl implements DrawService {

    private final EventMapper eventMapper;
    private final PrizeMapper prizeMapper;
    private final TicketPoolMapper ticketPoolMapper;

    /**
     * 당첨 산정 실행
     * 
    * 당첨 산정으로 Prize 레코드를 대량 생성합니다.
     * 
     * 동작 로직:
     * 1. 당첨 결과 슬롯 감지 (대상 1000명)
     * 2. ticket_pool에서 당첨된 항목(rank ≥ 1) 검색
     * 3. 각 항목에 대해 Prize 레코드 생성
     * 4. 대량 삽입
     * 
     * @param eventId 당첨 산정을 실행할 이벤트 ID
    * @return 당첨 산정 결과 (등수별 개수 포함)
     * @throws ApiException 당첨 대상 부족 또는 이벤트 미존재 시
     */
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

    /**
    * 당첨 결과 응답 구성
     * 
     * @param eventId 당첨 이벤트 ID
     * @return 당첨 결과 DTO
     */
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
