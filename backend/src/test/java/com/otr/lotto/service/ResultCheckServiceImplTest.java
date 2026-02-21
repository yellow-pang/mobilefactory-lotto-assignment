package com.otr.lotto.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import com.otr.lotto.domain.Prize;
import com.otr.lotto.dto.ParticipateRequest;
import com.otr.lotto.dto.ResultCheckRequest;
import com.otr.lotto.dto.ResultCheckResponse;
import com.otr.lotto.mapper.PrizeMapper;
import com.otr.lotto.mapper.TicketPoolMapper;
import com.otr.lotto.serviceImpl.ParticipationServiceImpl;
import com.otr.lotto.serviceImpl.ResultCheckServiceImpl;
import com.otr.lotto.serviceImpl.TicketPoolServiceImpl;
import com.otr.lotto.support.TestDateConfig;

@SpringBootTest
@Transactional
@Import(TestDateConfig.class)
@DisplayName("로또 결과 조회 서비스 테스트")
class ResultCheckServiceImplTest {

    @Autowired
    private ResultCheckServiceImpl resultCheckService;

    @Autowired
    private ParticipationServiceImpl participationService;

    @Autowired
    private PrizeMapper prizeMapper;

    @Autowired
    private TicketPoolMapper ticketPoolMapper;

    @Autowired
    private TicketPoolServiceImpl ticketPoolService;

    private Long eventId = 1L;

    @BeforeEach
    void setUp() {
        setAnnounceDate();
        // 당첨자 초기화
        prizeMapper.deleteByEvent(eventId);
        if (ticketPoolMapper.countByEvent(eventId) == 0) {
            ticketPoolService.preparePool(eventId);
        }
    }

    private void setEventDate() {
        TestDateConfig.setFixedDate(java.time.LocalDate.of(2025, 2, 15));
    }

    private void setAnnounceDate() {
        TestDateConfig.setFixedDate(java.time.LocalDate.of(2025, 4, 5));
    }

    @Test
    @DisplayName("첫 조회 - rank값 반환")
    void testCheck_FirstVisit_ReturnsRank() {
        // Given: 참여한 사용자
        setEventDate();
        ParticipateRequest participateReq = new ParticipateRequest();
        participateReq.setPhone("010-1111-1111");
        participationService.participate(participateReq);

        // When: 첫 번째 조회
        setAnnounceDate();
        ResultCheckRequest checkReq = new ResultCheckRequest();
        checkReq.setPhone("010-1111-1111");
        ResultCheckResponse checkRes = resultCheckService.check(checkReq);

        // Then
        assertEquals(1, checkRes.getCheckCount(), "첫 조회는 checkCount=1");
        Integer rank = checkRes.getRank();
        if (rank != null) {
            assertTrue(rank >= 1 && rank <= 4, "rank는 1~4 사이여야 함");
        }
    }

    @Test
    @DisplayName("두 번째 조회 - isWinner만 반환, rank는 null")
    void testCheck_SecondVisit_ReturnsIsWinnerOnly() {
        // Given: 참여한 사용자
        setEventDate();
        ParticipateRequest participateReq = new ParticipateRequest();
        participateReq.setPhone("010-2222-2222");
        participationService.participate(participateReq);

        // When: 첫 번째 조회
        setAnnounceDate();
        ResultCheckRequest checkReq = new ResultCheckRequest();
        checkReq.setPhone("010-2222-2222");
        ResultCheckResponse checkRes1 = resultCheckService.check(checkReq);

        // And: 두 번째 조회
        ResultCheckResponse checkRes2 = resultCheckService.check(checkReq);

        // Then
        assertEquals(2, checkRes2.getCheckCount(), "두 번째 조회는 checkCount=2");
        assertNotNull(checkRes2.getIsWinner(), "isWinner값이 반환되어야 함");
        assertFalse(checkRes2.getIsWinner(), "미당첨자는 isWinner=false");
    }

    @Test
    @DisplayName("당첨자 확인 - rank 1~4 중 하나 반환")
    void testCheck_WinnerRank() {
        // Given: 1등 보장 휴대폰
        setEventDate();
        ParticipateRequest participateReq = new ParticipateRequest();
        participateReq.setPhone("010-1234-5678"); // fixed_first_phone_hash
        var participateRes = participationService.participate(participateReq);

        Prize prize = new Prize();
        prize.setEventId(eventId);
        prize.setParticipantId(participateRes.getParticipantId());
        prize.setRank(1);
        prizeMapper.insertBatch(List.of(prize));

        // When
        setAnnounceDate();
        ResultCheckRequest checkReq = new ResultCheckRequest();
        checkReq.setPhone("010-1234-5678");
        ResultCheckResponse checkRes = resultCheckService.check(checkReq);

        // Then: rank는 1~4 중 하나여야 함
        Integer rank = checkRes.getRank();
        assertNotNull(rank, "당첨자는 rank가 있어야 함");
        assertEquals(1, rank, "당첨자는 1등으로 설정됨");
    }

    @Test
    @DisplayName("미당첨자 확인 - rank가 null 또는 isWinner=false")
    void testCheck_NonWinnerRank() {
        // Given: 임의의 참여자 (대부분 미당첨)
        setEventDate();
        ParticipateRequest participateReq = new ParticipateRequest();
        participateReq.setPhone("010-3333-3333");
        participationService.participate(participateReq);

        // When
        setAnnounceDate();
        ResultCheckRequest checkReq = new ResultCheckRequest();
        checkReq.setPhone("010-3333-3333");
        ResultCheckResponse checkRes = resultCheckService.check(checkReq);

        // Then: 미당첨자는 rank=null 또는 isWinner=false
        assertEquals(null, checkRes.getRank(), "첫 조회에서 미당첨자는 rank=null");
    }

    @Test
    @DisplayName("조회 횟수 증가 - checkCount 증가 확인")
    void testCheck_CheckCountIncrement() {
        // Given
        setEventDate();
        ParticipateRequest participateReq = new ParticipateRequest();
        participateReq.setPhone("010-4444-4444");
        participationService.participate(participateReq);

        ResultCheckRequest checkReq = new ResultCheckRequest();
        checkReq.setPhone("010-4444-4444");

        // When & Then
        setAnnounceDate();
        ResultCheckResponse check1 = resultCheckService.check(checkReq);
        assertEquals(1, check1.getCheckCount());

        ResultCheckResponse check2 = resultCheckService.check(checkReq);
        assertEquals(2, check2.getCheckCount());

        ResultCheckResponse check3 = resultCheckService.check(checkReq);
        assertEquals(3, check3.getCheckCount());
    }

    @Test
    @DisplayName("참여하지 않은 사용자 확인 - NOT_FOUND 에러")
    void testCheck_NotParticipant() {
        // Given: 참여하지 않은 사용자
        setAnnounceDate();
        ResultCheckRequest checkReq = new ResultCheckRequest();
        checkReq.setPhone("010-9999-9999");

        // When & Then
        assertThrows(Exception.class, () -> {
            resultCheckService.check(checkReq);
        }, "참여하지 않은 사용자는 조회 불가");
    }
}
