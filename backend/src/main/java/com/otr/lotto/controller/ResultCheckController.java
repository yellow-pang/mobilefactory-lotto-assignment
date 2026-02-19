package com.otr.lotto.controller;

import java.time.LocalDate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.otr.lotto.common.ApiException;
import com.otr.lotto.common.ApiResponse;
import com.otr.lotto.common.ErrorCode;
import com.otr.lotto.domain.Event;
import com.otr.lotto.dto.ResultCheckRequest;
import com.otr.lotto.dto.ResultCheckResponse;
import com.otr.lotto.mapper.EventMapper;
import com.otr.lotto.service.ResultCheckService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/results")
@RequiredArgsConstructor
public class ResultCheckController {

    private final ResultCheckService resultCheckService;
    private final EventMapper eventMapper;

    /**
     * 발표 기간 확인
     * - 200 OK: 기간 내
     * - 404: 기간 외
     */
    @GetMapping("/check-period")
    public ApiResponse<Void> checkAnnouncePeriod() {
        Event event = eventMapper.findActiveAnnounceEvent(LocalDate.now());
        if (event == null) {
            throw new ApiException(ErrorCode.ANNOUNCE_NOT_ACTIVE);
        }
        return ApiResponse.success(null);
    }

    @PostMapping("/check")
    public ApiResponse<ResultCheckResponse> check(@Valid @RequestBody ResultCheckRequest request) {
        return ApiResponse.success(resultCheckService.check(request));
    }
}
