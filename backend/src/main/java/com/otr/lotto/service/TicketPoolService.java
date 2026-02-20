package com.otr.lotto.service;

import com.otr.lotto.dto.TicketPoolPrepareResponse;

public interface TicketPoolService {
    TicketPoolPrepareResponse preparePool(Long eventId);
}
