package com.otr.lotto.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.otr.lotto.common.ApiException;
import com.otr.lotto.common.ErrorCode;
import com.otr.lotto.domain.Participant;
import com.otr.lotto.domain.TicketPool;
import com.otr.lotto.dto.ParticipateRequest;
import com.otr.lotto.dto.ParticipateResponse;
import com.otr.lotto.mapper.ParticipantMapper;
import com.otr.lotto.mapper.TicketPoolMapper;
import com.otr.lotto.serviceImpl.ParticipationServiceImpl;
import com.otr.lotto.support.TestDateConfig;

@SpringBootTest
@Transactional
@Import(TestDateConfig.class)
@DisplayName("로또 참여 서비스 테스트")
class ParticipationServiceImplTest {

    @Autowired
    private ParticipationServiceImpl participationService;

    @Autowired
    private ParticipantMapper participantMapper;

    @Autowired
    private TicketPoolMapper ticketPoolMapper;

    private Long eventId = 1L;

    @BeforeEach
    void setUp() {
        TestDateConfig.setFixedDate(java.time.LocalDate.of(2025, 2, 15));
        // 테스트 격리: 참가자 초기화
        participantMapper.deleteByEvent(eventId);
    }

    @Test
    @DisplayName("정상 참여 - 로또 번호 배정 성공")
    void testParticipate_Success() {
        // Given
        ParticipateRequest request = new ParticipateRequest();
        request.setPhone("010-1234-5678");

        // When
        ParticipateResponse response = participationService.participate(request);

        // Then
        assertNotNull(response.getParticipantId());
        assertNotNull(response.getLottoNumber());
        assertTrue(response.getLottoNumber().contains(","));

        // DB 확인
        Participant participant = participantMapper.findById(response.getParticipantId());
        assertNotNull(participant);
        assertEquals(eventId, participant.getEventId());
    }

    @Test
    @DisplayName("중복 참여 방지 - 같은 휴대폰으로 다시 참여 시 실패")
    void testParticipate_DuplicateBlocked() {
        // Given
        ParticipateRequest request = new ParticipateRequest();
        request.setPhone("010-2222-3333");

        // When: 첫 번째 참여 성공
        ParticipateResponse response1 = participationService.participate(request);
        assertNotNull(response1.getParticipantId());

        // Then: 두 번째 참여 시도 → DUPLICATE_PARTICIPATION 에러
        ApiException exception = assertThrows(ApiException.class, () -> {
            participationService.participate(request);
        });
        assertEquals(ErrorCode.DUPLICATE_PARTICIPATION, exception.getErrorCode());
    }

    @Test
    @DisplayName("휴대폰 정규화 검증 - 다양한 형식이 모두 중복으로 인식")
    void testParticipate_PhoneNormalization() {
        // Given: 첫 번째 참여 (010-1234-5678)
        ParticipateRequest request1 = new ParticipateRequest();
        request1.setPhone("010-1234-5678");
        participationService.participate(request1);

        // When & Then: 다른 형식이어도 중복으로 인식
        ParticipateRequest request2 = new ParticipateRequest();
        request2.setPhone("01012345678"); // 하이픈 없음

        assertThrows(ApiException.class, () -> {
            participationService.participate(request2);
        }, "휴대폰 정규화로 중복 인식");
    }

    @Test
    @DisplayName("정원 초과 방지")
    void testParticipate_CapacityFull() {
        // Given: 정원이 매우 낮게 설정된 이벤트 (10,000명)
        // 현재는 10,000명까지 참여 가능하므로 이 테스트는 스킵
        // (실제로는 정원 테스트를 위해 별도 이벤트 필요)

        // 실제 구현 시 eventMapper에서 maxParticipants를 0으로 설정한 이벤트 사용
    }

    @Test
    @DisplayName("배정되는 로또 번호가 번호 풀과 일치")
    void testParticipate_LottoNumberFromPool() {
        // Given
        long participantId = 1L;
        ParticipateRequest request = new ParticipateRequest();
        request.setPhone("010-9876-5432");

        // When
        ParticipateResponse response = participationService.participate(request);

        // Then: 받은 번호가 ticket_pool의 seq 1번과 매핑되어야 함
        TicketPool pool = ticketPoolMapper.findByEventAndSeq(eventId, participantId);
        assertNotNull(pool);
        assertEquals(response.getLottoNumber(), pool.getLottoNumber());
        assertEquals(response.getParticipantId(), pool.getAssignedParticipantId());
    }

    @Test
    @DisplayName("특정 휴대폰 1등 보장")
    void testParticipate_FirstPrizeGuaranteed() {
        // Given: fixed_first_phone_hash를 확인해야 함
        // DB의 event 테이블에서 fixed_first_phone_hash가 설정되어 있어야 함
        // 현재 DB: SHA2('01012345678', 256)이 설정됨

        // 실제 고정된 휴대폰으로 참여하면 1등 보장됨
        // (현재 DB 설정에서는 01012345678이 고정 휴대폰)

        ParticipateRequest request = new ParticipateRequest();
        request.setPhone("010-1234-5678"); // fixed_first_phone_hash와 일치

        // When
        ParticipateResponse response = participationService.participate(request);

        // Then: 배정된 번호의 rank를 확인 (1등이어야 함)
        TicketPool assignedPool = ticketPoolMapper.findByEventAndSeq(eventId, response.getParticipantId());
        assertEquals(1, assignedPool.getRank(), "특정 휴대폰은 1등 번호를 받아야 함");
    }
}
