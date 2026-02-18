package com.otr.lotto.service;

import com.otr.lotto.dto.ReminderResponse;

public interface ReminderService {
    /**
     * 미확인 당첨자 안내 발송
     * 
     * @param eventId 이벤트 ID
     * @return 안내 발송 결과
     */
    ReminderResponse sendUnconfirmedWinnerReminders(Long eventId);
}
