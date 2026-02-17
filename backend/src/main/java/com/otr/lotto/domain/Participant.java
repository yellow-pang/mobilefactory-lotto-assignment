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
public class Participant {
    private Long id;
    private Long eventId;
    private String phoneHash;
    private Integer checkCount;
    private LocalDateTime firstCheckedAt;
    private LocalDateTime lastCheckedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
