package com.otr.lotto.service;

import com.otr.lotto.dto.ParticipateRequest;
import com.otr.lotto.dto.ParticipateResponse;

/**
 * 로또 이벤트 참여 서비스 인터페이스
 * 
 * 사용자의 휴대폰 번호 기반 참여 신청을 처리합니다.
 */
public interface ParticipationService {
    /**
     * 로또 이벤트 참여 신청
     * 
     * @param request 휴대폰 번호를 포함한 참여 요청
     * @return 참여순번과 배정된 로또 번호
     */
    ParticipateResponse participate(ParticipateRequest request);
}
