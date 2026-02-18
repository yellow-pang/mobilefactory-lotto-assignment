package com.otr.lotto.service;

import com.otr.lotto.dto.ParticipateRequest;
import com.otr.lotto.dto.ParticipateResponse;

public interface ParticipationService {
    ParticipateResponse participate(ParticipateRequest request);
}
