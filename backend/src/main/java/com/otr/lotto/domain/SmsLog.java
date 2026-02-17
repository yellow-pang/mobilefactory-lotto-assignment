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
public class SmsLog {
    private Long id;
    private Long participantId;
    private String phone;
    private String message;
    private LocalDateTime sentDate;
}
