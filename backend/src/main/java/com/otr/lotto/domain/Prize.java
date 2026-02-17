package com.otr.lotto.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Prize {
    private Long id;
    private Long eventId;
    private Integer rank;
    private Long amount;
    private Integer winnersCount;
}
