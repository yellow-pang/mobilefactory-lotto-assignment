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

        // 3. 당첨 번호 결정 (설정값)
        List<Integer> winningNumbers = getWinningNumbers(event);
        Participant firstPrizeWinner = findFirstPrizeWinner(eventId, winningNumbers);
        if (firstPrizeWinner == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "당첨 번호와 일치하는 참가자가 없습니다.");
        }

        // 4. 당첨자 리스트 생성
        List<Prize> prizes = new ArrayList<>();
        Set<Long> selectedParticipantIds = new HashSet<>();

        // 4-1. 1등 추가
        prizes.add(createPrize(eventId, firstPrizeWinner.getId(), 1));
        selectedParticipantIds.add(firstPrizeWinner.getId());

        // 4-2. 모든 참가자 조회
        List<Participant> allParticipants = participantMapper.findAllByEvent(eventId);
        allParticipants.removeIf(p -> selectedParticipantIds.contains(p.getId()));

        // 4-3. 모든 참가자의 티켓 정보 조회
        List<Long> allParticipantIds = allParticipants.stream()
            .map(Participant::getId)
            .collect(Collectors.toList());
        
        List<Ticket> allTickets = ticketMapper.findByParticipantIds(allParticipantIds);
        Map<Long, String> ticketMap = new HashMap<>();
        for (Ticket ticket : allTickets) {
            ticketMap.put(ticket.getParticipantId(), ticket.getLottoNumber());
        }

        // 4-4. 자리수 일치 개수별로 참가자 분류
        Map<Integer, List<Participant>> matchCountGroups = new HashMap<>();
        for (int i = 6; i >= 0; i--) {
            matchCountGroups.put(i, new ArrayList<>());
        }

        for (Participant p : allParticipants) {
            String lottoNumber = ticketMap.get(p.getId());
            if (lottoNumber != null) {
                int matchCount = countMatchingNumbers(winningNumbers, lottoNumber);
                matchCountGroups.get(matchCount).add(p);
            }
        }

        // 4-5. 2등 (5명): 참여자 번호 2000~7000 범위 내에서 선정
        Set<Long> secondEligibleIds = allParticipants.stream()
            .map(Participant::getId)
            .filter(id -> id >= 2000 && id <= 7000)
            .collect(Collectors.toSet());
        List<Prize> secondPrizes = selectPrizesByMatchCount(
            eventId,
            matchCountGroups,
            5,
            2,
            secondEligibleIds
        );
        prizes.addAll(secondPrizes);
        secondPrizes.forEach(p -> selectedParticipantIds.add(p.getParticipantId()));
        updateAvailableCandidates(matchCountGroups, selectedParticipantIds);

        // 4-6. 3등 (44명): 참여자 번호 1000~8000 범위 내에서 선정
        Set<Long> thirdEligibleIds = allParticipants.stream()
            .map(Participant::getId)
            .filter(id -> id >= 1000 && id <= 8000)
            .collect(Collectors.toSet());
        List<Prize> thirdPrizes = selectPrizesByMatchCount(
            eventId,
            matchCountGroups,
            4,
            3,
            thirdEligibleIds
        );
        prizes.addAll(thirdPrizes);
        thirdPrizes.forEach(p -> selectedParticipantIds.add(p.getParticipantId()));
        updateAvailableCandidates(matchCountGroups, selectedParticipantIds);

        // 4-7. 4등 (950명): 전체 참여자 대상
        List<Prize> fourthPrizes = selectPrizesByMatchCount(eventId, matchCountGroups, 3, 4, null);
        prizes.addAll(fourthPrizes);

        // 5. Prize 테이블에 일괄 삽입
        if (!prizes.isEmpty()) {
            prizeMapper.insertBatch(prizes);
        }

        // 6. 결과 반환
        return buildDrawResponse(eventId);
    }

    /**
     * 당첨 번호와 일치하는 1등 당첨자 찾기
     */
    private Participant findFirstPrizeWinner(Long eventId, List<Integer> winningNumbers) {
        List<Participant> allParticipants = participantMapper.findAllByEvent(eventId);
        List<Ticket> allTickets = ticketMapper.findByParticipantIds(
            allParticipants.stream().map(Participant::getId).collect(Collectors.toList())
        );

        // 당첨 번호와 정확히 일치하는 참가자 찾기
        for (Ticket ticket : allTickets) {
            if (countMatchingNumbers(winningNumbers, ticket.getLottoNumber()) == 6) {
                return allParticipants.stream()
                    .filter(p -> p.getId().equals(ticket.getParticipantId()))
                    .findFirst()
                    .orElse(null);
            }
        }
        
        return null;
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

    /**
     * 등수별 당첨자 선정 (자리수 일치 기준)
     * 
     * 전략:
     * 1. matchCount 이상 일치하는 사람들 우선 선택
     * 2. 부족하면 matchCount-1, matchCount-2... 순으로 보충
     */
    private List<Prize> selectPrizesByMatchCount(
        Long eventId,
        Map<Integer, List<Participant>> matchCountGroups,
        int targetMatchCount,
        int rank,
        Set<Long> allowedParticipantIds
    ) {
        int requiredCount = getRequiredCount(rank);
        List<Prize> prizes = new ArrayList<>();

        // 우선으로 targetMatchCount부터 시작해서 낮은 수로 내려가며 선택
        for (int matchCount = targetMatchCount; matchCount >= 0 && prizes.size() < requiredCount; matchCount--) {
            List<Participant> candidates = matchCountGroups.getOrDefault(matchCount, new ArrayList<>());
            if (allowedParticipantIds != null) {
                candidates = candidates.stream()
                    .filter(p -> allowedParticipantIds.contains(p.getId()))
                    .collect(Collectors.toList());
            }
            
            if (!candidates.isEmpty()) {
                Collections.shuffle(candidates);
                int needed = requiredCount - prizes.size();
                int toTake = Math.min(needed, candidates.size());
                
                for (int i = 0; i < toTake; i++) {
                    prizes.add(createPrize(eventId, candidates.get(i).getId(), rank));
                }
            }
        }

        if (allowedParticipantIds != null && prizes.size() < requiredCount) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "조건을 만족하는 당첨자가 부족합니다.");
        }

        return prizes;
    }

    /**
     * 다음 등수 선택을 위해 이미 선택된 참가자 제거
     */
    private void updateAvailableCandidates(
        Map<Integer, List<Participant>> matchCountGroups,
        Set<Long> selectedParticipantIds
    ) {
        for (List<Participant> list : matchCountGroups.values()) {
            list.removeIf(p -> selectedParticipantIds.contains(p.getId()));
        }
    }

    /**
     * 번호 일치 개수 계산 (1~45 번호 기준)
     */
    private int countMatchingNumbers(List<Integer> winningNumbers, String lottoNumbersCsv) {
        Set<Integer> winningSet = new HashSet<>(winningNumbers);
        List<Integer> lottoNumbers = parseNumbers(lottoNumbersCsv);
        int count = 0;

        for (int number : lottoNumbers) {
            if (winningSet.contains(number)) {
                count++;
            }
        }

        return count;
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
