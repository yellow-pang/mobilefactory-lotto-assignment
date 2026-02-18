package com.otr.lotto.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReminderResponse {
    private Long eventId;
    private LocalDate targetDate;
    private Integer totalUnconfirmedWinners;
    private Integer remindersSent;
    private Integer remindersFailed;
}
