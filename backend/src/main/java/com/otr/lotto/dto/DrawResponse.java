package com.otr.lotto.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 당첨 산정 실행 응답
 * 
 * AdminDrawController POST /{eventId}/draw API 응답 DTO
 * 
 * 당첨 산정 후 1등부터 4등까지 등상별 당첨자 수 반환
 * 
 * 특징:
 * - 고정 값: totalWinners(1000), firstPrizeCount(1), secondPrizeCount(5), 
 *   thirdPrizeCount(44), fourthPrizeCount(950)
 * - 이 값들은 ticket_pool 생성 시 미리 정해진 값
 * - 당첨 산정 로직은 단순히 DB에 Prize 레코드 일괄 삽입
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DrawResponse {
    private Long eventId;
    private Integer totalWinners;
    private Integer firstPrizeCount;
    private Integer secondPrizeCount;
    private Integer thirdPrizeCount;
    private Integer fourthPrizeCount;
}
