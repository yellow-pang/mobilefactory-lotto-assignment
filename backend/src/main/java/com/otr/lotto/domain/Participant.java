package com.otr.lotto.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Participant {
    private Long id;
    private Long eventId;
    private String phone;
    private LocalDateTime participateDate;
}
