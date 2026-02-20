package com.otr.lotto.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 로또 당첨자 레코드
 * 
 * 당첨 산정 시 ticket_pool의 당첨 정보(rank ≥ 1)를 읽어
 * Prize 테이블에 기록합니다.
 * 
 * 특징:
 * - 참여자와 당첨 순위를 연결
 * - 같은 참여자가 여러 번 당첨될 수는 없음 (1인 1등상)
 * - rank는 1~4 (0은 저장되지 않음, 비당첨은 Prize 레코드 미생성)
 * - 당첨 산정 후 부상 지급 로직에서 참조
 * 
 * TicketPool과의 관계:
 * - ticket_pool.rank(1~4) → Prize.rank
 * - ticket_pool.assignedParticipantId → Prize.participantId
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Prize {
    private Long id;
    private Long eventId;
    private Long participantId;
    private Integer rank;
    private LocalDateTime createdAt;
}
