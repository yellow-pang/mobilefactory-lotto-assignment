package com.otr.lotto.service;

import com.otr.lotto.dto.ResultCheckRequest;
import com.otr.lotto.dto.ResultCheckResponse;

/**
 * 로또 당첨 결과 조회 서비스 인터페이스
 * 
 * 참여자의 해당 번호와 당첨/비당첨 결과를 조회합니다.
 */
public interface ResultCheckService {
    /**
     * 당첨 결과 조회
     * 
     * @param request 휴대폰 번호 기반 조회 요청
     * @return 조회 결과 (당첨 여부, 번호, 조회 횟수 등)
     */
    ResultCheckResponse check(ResultCheckRequest request);
}
