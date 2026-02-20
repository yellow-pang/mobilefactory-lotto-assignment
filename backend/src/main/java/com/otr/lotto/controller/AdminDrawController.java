package com.otr.lotto.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.otr.lotto.common.ApiResponse;
import com.otr.lotto.dto.DrawResponse;
import com.otr.lotto.dto.ReminderResponse;
import com.otr.lotto.dto.TicketPoolPrepareResponse;
import com.otr.lotto.service.DrawService;
import com.otr.lotto.service.ReminderService;
import com.otr.lotto.service.TicketPoolService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/events")
@RequiredArgsConstructor
public class AdminDrawController {

    private final DrawService drawService;
    private final ReminderService reminderService;
    private final TicketPoolService ticketPoolService;

    /**
     * 당첨 산정 실행
     * 
     * @param eventId 이벤트 ID
     * @return 당첨 산정 결과
     */
    @PostMapping("/{eventId}/draw")
    public ApiResponse<DrawResponse> executeDraw(@PathVariable Long eventId) {
        DrawResponse response = drawService.executeDraw(eventId);
        return ApiResponse.success(response);
    }

    /**
     * 번호 풀 사전 생성
     *
     * @param eventId 이벤트 ID
     * @return 생성 결과
     */
    @PostMapping("/{eventId}/prepare-tickets")
    public ApiResponse<TicketPoolPrepareResponse> prepareTickets(@PathVariable Long eventId) {
        TicketPoolPrepareResponse response = ticketPoolService.preparePool(eventId);
        return ApiResponse.success(response);
    }

    /**
     * 미확인 당첨자 안내 발송
     * 
     * @param eventId 이벤트 ID
     * @return 안내 발송 결과
     */
    @PostMapping("/{eventId}/remind-unconfirmed")
    public ApiResponse<ReminderResponse> sendUnconfirmedReminders(@PathVariable Long eventId) {
        ReminderResponse response = reminderService.sendUnconfirmedWinnerReminders(eventId);
        return ApiResponse.success(response);
    }
}
