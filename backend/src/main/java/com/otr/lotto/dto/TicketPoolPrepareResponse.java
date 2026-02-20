package com.otr.lotto.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketPoolPrepareResponse {
    private Long eventId;
    private int totalTickets;
}
