package com.otr.lotto.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 번호 풀 사전 생성 응답
 * 
 * AdminDrawController POST /{eventId}/prepare-tickets API 응답 DTO
 * 
 * 10,000개 로또 번호 풀 사전 생성 결과를 반환합니다.
 * 
 * 형식:
 * - eventId: 이벤트 ID
 * - totalTickets: 생성된 번호 수
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketPoolPrepareResponse {
    private Long eventId;
    private int totalTickets;
}
