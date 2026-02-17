package com.otr.lotto.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SmsLog {
    private Long id;
    private Long eventId;
    private Long participantId;
    private String phoneHash;
    private String type;       // PARTICIPATION_NUMBER / UNCONFIRMED_WINNER_REMINDER
    private LocalDate sentDate; // 중복 방지용(날짜 단위)
    private String status;      // SENT / FAILED
    private LocalDateTime createdAt;
}
