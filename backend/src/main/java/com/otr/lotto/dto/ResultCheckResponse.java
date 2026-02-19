package com.otr.lotto.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
