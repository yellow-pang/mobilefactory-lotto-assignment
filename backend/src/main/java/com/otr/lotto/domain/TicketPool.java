package com.otr.lotto.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketPool {
    private Long id;
    private Long eventId;
    private Long seq;
    private String lottoNumber;
    private Integer rank; // 0 = non-winner, 1~4 = winner
    private Long assignedParticipantId;
    private LocalDateTime createdAt;
}
