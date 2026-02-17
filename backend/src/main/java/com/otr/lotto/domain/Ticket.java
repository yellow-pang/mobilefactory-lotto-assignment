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
public class Ticket {
    private Long id;
    private Long eventId;
    private Long participantId;
    private String lottoNumber;
    private LocalDateTime createdAt;
}
