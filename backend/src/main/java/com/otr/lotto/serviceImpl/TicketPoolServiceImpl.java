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

/**
 * 로또 번호 풀 사전 생성 서비스
 * 
 * 이벤트당 10,000개의 로또 번호를 사전에 생성하고 rank를 배정하는 서비스입니다.
 * 이를 통해 참여시점에 번호가 확정되고, draw 시점에는 단순히 기록만 남깁니다.
 * 
 * 주요 책임:
 * - ticket_pool 테이블에 10,000 레코드 생성
 * - rank 배분: 1등(1명), 2등(5명, seq 2000~7000), 3등(44명, seq 1000~8000), 4등(950명)
 * - 각 rank에 맞는 로또 번호 생성 (당첨번호 일치 개수 기준)
 * - 구간 제한 로직: 2등/3등은 특정 seq 범위에서만 생성
 */
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

    /**
     * 번호 풀 사전 생성
     * 
     * 10,000개의 로또 번호에 rank를 배정하여 ticket_pool 테이블에 저장합니다.
     * 
     * 생성 과정:
     * 1. 이벤트 정보 조회 (당첨번호 확인)
     * 2. 중복 생성 방지 검사
     * 3. rank 별 seq 무작위 선별:
     *    - 1등(rank=1): seq 1~10,000 중에서 1개
     *    - 2등(rank=2): seq 2,000~7,000 중에서 5개
     *    - 3등(rank=3): seq 1,000~8,000 중에서 44개
     *    - 4등(rank=4): seq 1~10,000 중에서 950개
     *    - 0등(rank=0): 나머지 (비당첨, 9,000개)
     * 4. 각 seq별로 rank에 맞는 번호 생성
     * 5. 10,000 레코드 대량 삽입
     * 
     * @param eventId 번호 풀을 생성할 이벤트 ID
     * @return 생성 결과 (이벤트ID, 생성된 항목 수)
     * @throws ApiException 이벤트 미존재, 중복 생성, 숫자 부족 등
     */
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

    /**
     * 구간 제한이 있는 무작위 seq 선별
     * 
     * 지정된 범위(start~end) 내에서 제외 목록을 피하며
     * 무작위로 count 개의 seq를 선택합니다.
     * 
     * 예시:
     * - pickRandomSeqs(2000, 7000, 5, rankOneSeqs)
     *   → 2000~7000 범위에서 5개 선택, rankOneSeqs와 중복 제외
     * 
     * @param start 범위 시작 (포함)
     * @param end 범위 끝 (포함)
     * @param count 선택할 개수
     * @param exclude 제외할 seq 목록
     * @return 선택된 seq 집합
     * @throws ApiException 범위 내 선택 가능 개수 부족 시
     */
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

    /**
     * 두 집합 합병
     * 
     * @param first 첫 번째 집합
     * @param second 두 번째 집합
     * @return first와 second의 합집합
     */
    private Set<Long> union(Set<Long> first, Set<Long> second) {
        Set<Long> merged = new HashSet<>(first);
        merged.addAll(second);
        return merged;
    }

    /**
     * 세 집합 합병
     * 
     * @param first 첫 번째 집합
     * @param second 두 번째 집합
     * @param third 세 번째 집합
     * @return 세 집합의 합집합
     */
    private Set<Long> union(Set<Long> first, Set<Long> second, Set<Long> third) {
        Set<Long> merged = new HashSet<>(first);
        merged.addAll(second);
        merged.addAll(third);
        return merged;
    }

    /**
     * 당첨 번호 문자열 파싱
     * 
     * event.winning_number 필드(쉼표 구분 CSV 형식)
     * 을 Integer 리스트로 변환합니다.
     * 
     * 검증:
     * - null/공백 불가
     * - 정확히 6개 숫자
     * - 중복 없음
     * - 1~45 범위
     * 
     * @param value 당첨 번호 (예: "3,11,22,33,41,45")
     * @return 파싱된 당첨 번호 리스트
     * @throws ApiException 검증 실패 시
     */
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

    /**
     * rank에 맞는 로또 번호 생성
     * 
     * rank 값에 따라 당첨번호와의 일치 개수를 조정합니다:
     * - rank 1 (1등): 모두 일치 (6개)
     * - rank 2 (2등): 5개 일치
     * - rank 3 (3등): 4개 일치
     * - rank 4 (4등): 3개 일치
     * - rank 0 (비당첨): 2개 이하 일치
     * 
     * @param winningNumbers 당첨번호 리스트
     * @param rank 순위
     * @return 생성된 로또 번호 (CSV 형식)
     */
    private String generateNumberForRank(List<Integer> winningNumbers, int rank) {
        return switch (rank) {
            case 1 -> formatNumbers(winningNumbers);
            case 2 -> generateVariantNumbers(winningNumbers, 5);
            case 3 -> generateVariantNumbers(winningNumbers, 4);
            case 4 -> generateVariantNumbers(winningNumbers, 3);
            default -> generateNonWinningNumbers(winningNumbers, 2);
        };
    }

    /**
     * 당첨번호와 일부만 일치하는 번호 생성
     * 
     * 당첨번호 중에서 matchCount개를 무작위로 선택하고
     * 비당첨번호에서 나머지를 채워 6개 번호를 완성합니다.
     * 
     * @param winningNumbers 당첨번호 리스트
     * @param matchCount 일치시킬 개수 (2, 3, 4, 5)
     * @return 생성된 로또 번호 (CSV 형식)
     */
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

    /**
     * 당첨번호와 거의 일치하지 않는 번호 생성
     * 
     * maxMatch 이하로만 일치하는 번호를 생성합니다.
     * 최대 1000번 시도합니다.
     * 
     * @param winningNumbers 당첨번호 리스트
     * @param maxMatch 최대 일치 개수 (2 이하)
     * @return 생성된 로또 번호 (CSV 형식)
     * @throws ApiException 1000번 시도 후에도 실패 시
     */
    private String generateNonWinningNumbers(List<Integer> winningNumbers, int maxMatch) {
        for (int attempt = 0; attempt < 1000; attempt++) {
            List<Integer> numbers = generateRandomNumbers();
            if (countMatchingNumbers(winningNumbers, numbers) <= maxMatch) {
                return formatNumbers(numbers);
            }
        }

        throw new ApiException(ErrorCode.INTERNAL_ERROR);
    }

    /**
     * 무작위 로또 번호 생성
     * 
     * 1~45 범위에서 중복 없이 6개를 생성하고 오름차순 정렬합니다.
     * 
     * @return 무작위 로또 번호 리스트 (오름차순 정렬)
     */
    private List<Integer> generateRandomNumbers() {
        Set<Integer> numbers = new HashSet<>();
        while (numbers.size() < 6) {
            numbers.add(ThreadLocalRandom.current().nextInt(1, 46));
        }
        List<Integer> result = new ArrayList<>(numbers);
        Collections.sort(result);
        return result;
    }

    /**
     * 당첨번호와의 일치 개수 산정
     * 
     * candidate 번호 중 winningNumbers에 포함된
     * 개수를 반환합니다.
     * 
     * @param winningNumbers 당첨번호 리스트
     * @param candidate 비교할 번호 리스트
     * @return 일치하는 개수 (0~6)
     */
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

    /**
     * 로또 번호 리스트를 CSV 문자열로 변환
     * 
     * [3, 11, 22, 33, 41, 45] → "3,11,22,33,41,45"
     * 
     * @param numbers 번호 리스트
     * @return CSV 형식의 번호 문자열
     */
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
