package com.otr.lotto.service;

import com.otr.lotto.dto.TicketPoolPrepareResponse;

/**
 * 로또 번호 풀 사전 생성 서비스 인터페이스
 * 
 * 이벤트별로 10,000개의 로또 번호를 미리 생성하고 rank를 배정합니다.
 */
public interface TicketPoolService {
    /**
     * 번호 풀 사전 생성
     * 
     * @param eventId 이벤트 ID
     * @return 생성 결과 (이벤트ID, 생성된 항목수)
     */
    TicketPoolPrepareResponse preparePool(Long eventId);
}
