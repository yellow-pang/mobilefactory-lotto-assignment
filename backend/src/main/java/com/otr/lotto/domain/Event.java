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
public class Event {
    private Long id;
    private String name;
    private LocalDate eventStart;
    private LocalDate eventEnd;
    private LocalDate announceStart;
    private LocalDate announceEnd;
    private Integer maxParticipants;
    private String fixedFirstPhoneHash;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
