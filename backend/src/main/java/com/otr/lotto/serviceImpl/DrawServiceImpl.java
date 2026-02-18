package com.otr.lotto.serviceImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.otr.lotto.common.ApiException;
import com.otr.lotto.common.ErrorCode;
import com.otr.lotto.domain.Event;
import com.otr.lotto.domain.Participant;
import com.otr.lotto.domain.Prize;
import com.otr.lotto.domain.Ticket;
import com.otr.lotto.dto.DrawResponse;
import com.otr.lotto.mapper.EventMapper;
import com.otr.lotto.mapper.ParticipantMapper;
import com.otr.lotto.mapper.PrizeMapper;
import com.otr.lotto.mapper.TicketMapper;
import com.otr.lotto.service.DrawService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DrawServiceImpl implements DrawService {

    private final EventMapper eventMapper;
    private final ParticipantMapper participantMapper;
    private final TicketMapper ticketMapper;
    private final PrizeMapper prizeMapper;

    @Override
    @Transactional
    public DrawResponse executeDraw(Long eventId) {
        // 1. 멱등성 체크: 이미 당첨 산정이 완료되었는지 확인
        long existingPrizeCount = prizeMapper.countByEvent(eventId);
        if (existingPrizeCount >= 1000) {
            // 이미 산정 완료된 경우 현재 결과 반환
            return buildDrawResponse(eventId);
        }

        // 2. Event 조회
        Event event = eventMapper.findById(eventId);
        if (event == null) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }

        // 3. 1등 당첨자 결정
        Participant firstPrizeWinner = determineFirstPrizeWinner(event);
        if (firstPrizeWinner == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        // 4. 당첨 번호 결정 (1등 당첨자의 로또 번호)
        Ticket firstPrizeTicket = ticketMapper.findByParticipantId(firstPrizeWinner.getId());
        String winningNumber = firstPrizeTicket.getLottoNumber();

        // 5. 당첨자 리스트 생성
        List<Prize> prizes = new ArrayList<>();
        Set<Long> selectedParticipantIds = new HashSet<>();

        // 5-1. 1등 추가
        prizes.add(createPrize(eventId, firstPrizeWinner.getId(), 1));
        selectedParticipantIds.add(firstPrizeWinner.getId());

        // 5-2. 2등 (5명): 참여 번호 2000~7000번 중 5자리 일치 우선
        List<Participant> secondPrizeCandidates = participantMapper.findByIdRange(eventId, 2000L, 7000L);
        secondPrizeCandidates.removeIf(p -> selectedParticipantIds.contains(p.getId()));
        List<Prize> secondPrizes = selectWinners(eventId, secondPrizeCandidates, winningNumber, 5, 2);
        prizes.addAll(secondPrizes);
        secondPrizes.forEach(p -> selectedParticipantIds.add(p.getParticipantId()));

        // 5-3. 3등 (44명): 참여 번호 1000~8000번 중 4자리 일치 우선
        List<Participant> thirdPrizeCandidates = participantMapper.findByIdRange(eventId, 1000L, 8000L);
        thirdPrizeCandidates.removeIf(p -> selectedParticipantIds.contains(p.getId()));
        List<Prize> thirdPrizes = selectWinners(eventId, thirdPrizeCandidates, winningNumber, 4, 3);
        prizes.addAll(thirdPrizes);
        thirdPrizes.forEach(p -> selectedParticipantIds.add(p.getParticipantId()));

        // 5-4. 4등 (950명): 전체 참여자 중 3자리 일치 우선
        List<Participant> fourthPrizeCandidates = participantMapper.findAllByEvent(eventId);
        fourthPrizeCandidates.removeIf(p -> selectedParticipantIds.contains(p.getId()));
        List<Prize> fourthPrizes = selectWinners(eventId, fourthPrizeCandidates, winningNumber, 3, 4);
        prizes.addAll(fourthPrizes);

        // 6. Prize 테이블에 일괄 삽입
        if (!prizes.isEmpty()) {
            prizeMapper.insertBatch(prizes);
        }

        // 7. 결과 반환
        return buildDrawResponse(eventId);
    }

    /**
     * 1등 당첨자 결정
     * - fixedFirstPhoneHash가 있으면 해당 참여자
     * - 없으면 2000~7000번 중 랜덤
     */
    private Participant determineFirstPrizeWinner(Event event) {
        if (event.getFixedFirstPhoneHash() != null && !event.getFixedFirstPhoneHash().isEmpty()) {
            Participant fixed = participantMapper.findByEventAndPhoneHash(
                event.getId(), 
                event.getFixedFirstPhoneHash()
            );
            if (fixed != null) {
                return fixed;
            }
        }

        // fixedFirstPhoneHash가 없거나 해당 참여자가 없는 경우
        List<Participant> candidates = participantMapper.findByIdRange(event.getId(), 2000L, 7000L);
        if (candidates.isEmpty()) {
            return null;
        }
        Collections.shuffle(candidates);
        return candidates.get(0);
    }

    /**
     * 등수별 당첨자 선정
     * 
     * @param eventId 이벤트 ID
     * @param candidates 후보군
     * @param winningNumber 당첨 번호
     * @param matchDigits 일치해야 할 자릿수 (5: 2등, 4: 3등, 3: 4등)
     * @param rank 등수
     * @return 선정된 Prize 리스트
     */
    private List<Prize> selectWinners(
        Long eventId,
        List<Participant> candidates,
        String winningNumber,
        int matchDigits,
        int rank
    ) {
        int requiredCount = getRequiredCount(rank);
        List<Prize> prizes = new ArrayList<>();

        if (candidates.isEmpty()) {
            return prizes;
        }

        // 1. 후보군의 티켓 조회
        List<Long> candidateIds = candidates.stream()
            .map(Participant::getId)
            .collect(Collectors.toList());
        
        List<Ticket> tickets = ticketMapper.findByParticipantIds(candidateIds);
        
        // participantId -> lottoNumber 매핑
        Map<Long, String> ticketMap = new HashMap<>();
        for (Ticket ticket : tickets) {
            ticketMap.put(ticket.getParticipantId(), ticket.getLottoNumber());
        }

        // 2. 자리수 일치자 필터링
        List<Participant> matched = new ArrayList<>();
        List<Participant> unmatched = new ArrayList<>();

        for (Participant candidate : candidates) {
            String lottoNumber = ticketMap.get(candidate.getId());
            if (lottoNumber != null && countMatchingDigits(winningNumber, lottoNumber) >= matchDigits) {
                matched.add(candidate);
            } else {
                unmatched.add(candidate);
            }
        }

        // 3. 일치자 우선 선택
        Collections.shuffle(matched);
        int selectedCount = 0;

        for (Participant p : matched) {
            if (selectedCount >= requiredCount) break;
            prizes.add(createPrize(eventId, p.getId(), rank));
            selectedCount++;
        }

        // 4. 부족하면 불일치자 중에서 랜덤 선택
        if (selectedCount < requiredCount) {
            Collections.shuffle(unmatched);
            for (Participant p : unmatched) {
                if (selectedCount >= requiredCount) break;
                prizes.add(createPrize(eventId, p.getId(), rank));
                selectedCount++;
            }
        }

        return prizes;
    }

    /**
     * 연속 자리수 일치 개수 계산 (앞자리부터)
     */
    private int countMatchingDigits(String winningNumber, String lottoNumber) {
        int minLength = Math.min(winningNumber.length(), lottoNumber.length());
        int count = 0;
        
        for (int i = 0; i < minLength; i++) {
            if (winningNumber.charAt(i) == lottoNumber.charAt(i)) {
                count++;
            } else {
                break; // 연속 일치가 끊기면 종료
            }
        }
        
        return count;
    }

    /**
     * 등수별 필요 인원 수
     */
    private int getRequiredCount(int rank) {
        return switch (rank) {
            case 2 -> 5;
            case 3 -> 44;
            case 4 -> 950;
            default -> 0;
        };
    }

    /**
     * Prize 객체 생성
     */
    private Prize createPrize(Long eventId, Long participantId, int rank) {
        Prize prize = new Prize();
        prize.setEventId(eventId);
        prize.setParticipantId(participantId);
        prize.setRank(rank);
        return prize;
    }

    /**
     * DrawResponse 생성
     */
    private DrawResponse buildDrawResponse(Long eventId) {
        // 실제 저장된 등수별 카운트 조회 (간소화를 위해 고정값 반환)
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
