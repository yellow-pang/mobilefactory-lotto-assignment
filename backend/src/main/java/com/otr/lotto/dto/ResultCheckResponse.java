package com.otr.lotto.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 당첨 결과 조회 응답
 * 
 * ParticipationController GET /{eventId}/check API 응답 DTO
 * 
 * 참여자의 당첨 여부와 상세 정보를 조회할 때 반환됩니다.
 * 조회 횟수에 따라 반환 필드가 다릅니다 (보안): 
 * 
 * 첫 번째 조회 (초기 확인):
 * - rank: 1등(rank=1) ~ 4등(rank=4), 비당첨(null/0)
 * - lottoNumber: 배정된 번호
 * - amount: null (미공개)
 * 
 * 두 번째 이후 조회 (상세 확인):
 * - isWinner: true/false (rank >= 1 여부)
 * - amount: 상금액
 * - rank: null (미공개)
 * 
 * 공통:
 * - checkCount: 총 조회 횟수
 * 
 * @JsonInclude(NON_NULL): null 필드는 JSON에 포함하지 않음
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultCheckResponse {
    // First view: rank 사용 (조회 초기)
    private Integer rank;
    
    // Second+ view: isWinner 사용 (상세 조회)
    private Boolean isWinner;
    
    // 공통 필드
    private String lottoNumber;
    private Long amount;
    
    // 조회 횟수
    private Integer checkCount;
}
