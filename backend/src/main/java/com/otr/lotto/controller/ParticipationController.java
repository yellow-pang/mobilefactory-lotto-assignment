package com.otr.lotto.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.otr.lotto.common.ApiException;
import com.otr.lotto.common.ApiResponse;
import com.otr.lotto.common.ErrorCode;
import com.otr.lotto.domain.Event;
import com.otr.lotto.dto.ParticipateRequest;
import com.otr.lotto.dto.ParticipateResponse;
import com.otr.lotto.mapper.EventMapper;
import com.otr.lotto.service.ParticipationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/participations")
@RequiredArgsConstructor
public class ParticipationController {

    private final ParticipationService participationService;
    private final EventMapper eventMapper;
    private final com.otr.lotto.common.CurrentDateProvider currentDateProvider;

    /**
     * 지른 이벤트 기간인지 확인
     * - 200 OK: 기간 내
     * - 404: 기간 외
     */
    @GetMapping("/check-period")
    public ApiResponse<Void> checkEventPeriod() {
        Event event = eventMapper.findActiveEvent(currentDateProvider.today());
        if (event == null) {
            throw new ApiException(ErrorCode.EVENT_NOT_ACTIVE);
        }
        return ApiResponse.success(null);
    }

    @PostMapping
    public ApiResponse<ParticipateResponse> participate(@Valid @RequestBody ParticipateRequest request) {
        return ApiResponse.success(participationService.participate(request));
    }
}
