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
 * 10,000개 당첸 번호 사전 생성 둈료 식 담리 반환
 * 
 * 게식
 * - 당첸 분서 단춙 뇌깐른 뿬d대루 컴다
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketPoolPrepareResponse {
    private Long eventId;
    private int totalTickets;
}
