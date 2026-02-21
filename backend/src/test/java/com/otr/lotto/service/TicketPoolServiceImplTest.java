package com.otr.lotto.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Import;

import com.otr.lotto.support.TestDateConfig;

import com.otr.lotto.dto.TicketPoolPrepareResponse;
import com.otr.lotto.mapper.EventMapper;
import com.otr.lotto.mapper.TicketPoolMapper;
import com.otr.lotto.serviceImpl.TicketPoolServiceImpl;

@SpringBootTest
@Transactional
@Import(TestDateConfig.class)
@DisplayName("로또 번호 풀 생성 서비스 테스트")
class TicketPoolServiceImplTest {

    @Autowired
    private TicketPoolServiceImpl ticketPoolService;

    @Autowired
    private TicketPoolMapper ticketPoolMapper;

    @Autowired
    private EventMapper eventMapper;

    private Long eventId = 1L;

    @BeforeEach
    void setUp() {
        TestDateConfig.setFixedDate(java.time.LocalDate.of(2025, 2, 15));
        // 기존 ticket_pool 초기화 (테스트 격리)
        ticketPoolMapper.deleteByEvent(eventId);
    }

    @Test
    @DisplayName("10,000개 로또 번호 생성 검증")
    void testPreparePool_Success() {
        // Given: 이벤트가 존재

        // When: 번호 풀 생성
        TicketPoolPrepareResponse response = ticketPoolService.preparePool(eventId);

        // Then: 10,000개 생성됨
        assertEquals(eventId, response.getEventId());
        assertEquals(10_000, response.getPoolSize());

        long totalCount = ticketPoolMapper.countByEvent(eventId);
        assertEquals(10_000, totalCount);
    }

    @Test
    @DisplayName("rank별 개수 검증 (1등:1, 2등:5, 3등:44, 4등:950)")
    void testPreparePool_RankDistribution() {
        // Given & When
        ticketPoolService.preparePool(eventId);

        // Then: rank 배분 확인
        long rank1Count = ticketPoolMapper.countByRank(eventId, 1);
        long rank2Count = ticketPoolMapper.countByRank(eventId, 2);
        long rank3Count = ticketPoolMapper.countByRank(eventId, 3);
        long rank4Count = ticketPoolMapper.countByRank(eventId, 4);
        long rank0Count = ticketPoolMapper.countByRank(eventId, 0);

        assertEquals(1, rank1Count, "1등은 1명");
        assertEquals(5, rank2Count, "2등은 5명");
        assertEquals(44, rank3Count, "3등은 44명");
        assertEquals(950, rank4Count, "4등은 950명");
        assertEquals(9_000, rank0Count, "비당첨은 9,000명");
    }

    @Test
    @DisplayName("2등 seq 범위 검증 (2000~7000)")
    void testPreparePool_SecondPrizeSeqRange() {
        // Given & When
        ticketPoolService.preparePool(eventId);

        // Then: 2등이 2000~7000 범위 내인지 확인
        long outOfRangeCount = ticketPoolMapper.countByRankAndOutOfSeqRange(eventId, 2, 2000, 7000);
        assertEquals(0, outOfRangeCount, "2등은 모두 2000~7000 범위 내여야 함");
    }

    @Test
    @DisplayName("3등 seq 범위 검증 (1000~8000)")
    void testPreparePool_ThirdPrizeSeqRange() {
        // Given & When
        ticketPoolService.preparePool(eventId);

        // Then: 3등이 1000~8000 범위 내인지 확인
        long outOfRangeCount = ticketPoolMapper.countByRankAndOutOfSeqRange(eventId, 3, 1000, 8000);
        assertEquals(0, outOfRangeCount, "3등은 모두 1000~8000 범위 내여야 함");
    }

    @Test
    @DisplayName("중복 생성 방지 (멱등성)")
    void testPreparePool_Idempotency() {
        // Given: 첫 번째 생성
        TicketPoolPrepareResponse response1 = ticketPoolService.preparePool(eventId);
        assertEquals(10_000, response1.getPoolSize());

        // When: 두 번째 생성 시도 → 예외 발생
        // Then
        assertThrows(Exception.class, () -> ticketPoolService.preparePool(eventId),
                "이미 존재하는 이벤트는 중복 생성 불가");
    }
}
