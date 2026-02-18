package com.otr.lotto.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.otr.lotto.common.ApiResponse;
import com.otr.lotto.dto.ResultCheckRequest;
import com.otr.lotto.dto.ResultCheckResponse;
import com.otr.lotto.service.ResultCheckService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/results/check")
@RequiredArgsConstructor
public class ResultCheckController {

    private final ResultCheckService resultCheckService;

    @PostMapping
    public ApiResponse<ResultCheckResponse> check(@Valid @RequestBody ResultCheckRequest request) {
        return ApiResponse.success(resultCheckService.check(request));
    }
}
