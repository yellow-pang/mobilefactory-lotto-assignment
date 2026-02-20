package com.otr.lotto.serviceImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.otr.lotto.common.ApiException;
import com.otr.lotto.common.ErrorCode;
import com.otr.lotto.domain.Event;
import com.otr.lotto.domain.TicketPool;
import com.otr.lotto.dto.TicketPoolPrepareResponse;
import com.otr.lotto.mapper.EventMapper;
import com.otr.lotto.mapper.TicketPoolMapper;
import com.otr.lotto.service.TicketPoolService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketPoolServiceImpl implements TicketPoolService {
    private static final int TOTAL_TICKETS = 10_000;
    private static final int RANK_ONE_COUNT = 1;
    private static final int RANK_TWO_COUNT = 5;
    private static final int RANK_THREE_COUNT = 44;
    private static final int RANK_FOUR_COUNT = 950;

    private final EventMapper eventMapper;
    private final TicketPoolMapper ticketPoolMapper;

    @Transactional
    @Override
    public TicketPoolPrepareResponse preparePool(Long eventId) {
        Event event = eventMapper.findById(eventId);
        if (event == null) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }

        long existingCount = ticketPoolMapper.countByEvent(eventId);
        if (existingCount > 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "이미 번호 풀이 생성되었습니다.");
        }

        List<Integer> winningNumbers = parseWinningNumbers(event.getWinningNumber());

        Set<Long> rankOneSeqs = pickRandomSeqs(1, TOTAL_TICKETS, RANK_ONE_COUNT, Collections.emptySet());
        Set<Long> rankTwoSeqs = pickRandomSeqs(2000, 7000, RANK_TWO_COUNT, rankOneSeqs);
        Set<Long> rankThreeSeqs = pickRandomSeqs(1000, 8000, RANK_THREE_COUNT, union(rankOneSeqs, rankTwoSeqs));
        Set<Long> used = union(rankOneSeqs, rankTwoSeqs, rankThreeSeqs);
        Set<Long> rankFourSeqs = pickRandomSeqs(1, TOTAL_TICKETS, RANK_FOUR_COUNT, used);

        List<TicketPool> pools = new ArrayList<>(TOTAL_TICKETS);
        for (long seq = 1; seq <= TOTAL_TICKETS; seq++) {
            int rank = 0;
            if (rankOneSeqs.contains(seq)) {
                rank = 1;
            } else if (rankTwoSeqs.contains(seq)) {
                rank = 2;
            } else if (rankThreeSeqs.contains(seq)) {
                rank = 3;
            } else if (rankFourSeqs.contains(seq)) {
                rank = 4;
            }

            String lottoNumber = generateNumberForRank(winningNumbers, rank);
            TicketPool pool = new TicketPool();
            pool.setEventId(eventId);
            pool.setSeq(seq);
            pool.setLottoNumber(lottoNumber);
            pool.setRank(rank);
            pool.setAssignedParticipantId(null);
            pools.add(pool);
        }

        ticketPoolMapper.insertBatch(pools);
        return new TicketPoolPrepareResponse(eventId, TOTAL_TICKETS);
    }

    private Set<Long> pickRandomSeqs(long start, long end, int count, Set<Long> exclude) {
        List<Long> candidates = new ArrayList<>();
        for (long seq = start; seq <= end; seq++) {
            if (!exclude.contains(seq)) {
                candidates.add(seq);
            }
        }
        if (candidates.size() < count) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "조건을 만족하는 참가자 번호가 부족합니다.");
        }
        Collections.shuffle(candidates);
        Set<Long> selected = new HashSet<>();
        for (int i = 0; i < count; i++) {
            selected.add(candidates.get(i));
        }
        return selected;
    }

    private Set<Long> union(Set<Long> first, Set<Long> second) {
        Set<Long> merged = new HashSet<>(first);
        merged.addAll(second);
        return merged;
    }

    private Set<Long> union(Set<Long> first, Set<Long> second, Set<Long> third) {
        Set<Long> merged = new HashSet<>(first);
        merged.addAll(second);
        merged.addAll(third);
        return merged;
    }

    private List<Integer> parseWinningNumbers(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "이벤트 당첨 번호가 설정되지 않았습니다.");
        }

        String[] parts = value.split(",");
        List<Integer> numbers = new ArrayList<>(parts.length);
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                numbers.add(Integer.valueOf(trimmed));
            }
        }
        if (numbers.size() != 6) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "이벤트 당첨 번호는 6개의 숫자여야 합니다.");
        }
        Set<Integer> unique = new HashSet<>(numbers);
        if (unique.size() != 6) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "이벤트 당첨 번호는 중복 없이 6개여야 합니다.");
        }
        for (int number : numbers) {
            if (number < 1 || number > 45) {
                throw new ApiException(ErrorCode.INVALID_REQUEST, "이벤트 당첨 번호는 1~45 범위여야 합니다.");
            }
        }
        return numbers;
    }

    private String generateNumberForRank(List<Integer> winningNumbers, int rank) {
        return switch (rank) {
            case 1 -> formatNumbers(winningNumbers);
            case 2 -> generateVariantNumbers(winningNumbers, 5);
            case 3 -> generateVariantNumbers(winningNumbers, 4);
            case 4 -> generateVariantNumbers(winningNumbers, 3);
            default -> generateNonWinningNumbers(winningNumbers, 2);
        };
    }

    private String generateVariantNumbers(List<Integer> winningNumbers, int matchCount) {
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
            if (countMatchingNumbers(winningNumbers, numbers) <= maxMatch) {
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

    private int countMatchingNumbers(List<Integer> winningNumbers, List<Integer> candidate) {
        Set<Integer> winningSet = new HashSet<>(winningNumbers);
        int matches = 0;
        for (int number : candidate) {
            if (winningSet.contains(number)) {
                matches++;
            }
        }
        return matches;
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
}
