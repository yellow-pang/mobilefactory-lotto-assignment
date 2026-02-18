package com.otr.lotto.service;

import com.otr.lotto.dto.ResultCheckRequest;
import com.otr.lotto.dto.ResultCheckResponse;

public interface ResultCheckService {
    ResultCheckResponse check(ResultCheckRequest request);
}
