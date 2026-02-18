package com.otr.lotto.service;

import com.otr.lotto.dto.DrawResponse;

public interface DrawService {
    /**
     * 당첨 산정 실행
     * 
     * @param eventId 이벤트 ID
     * @return 당첨 산정 결과
     */
    DrawResponse executeDraw(Long eventId);
}
