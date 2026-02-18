package com.otr.lotto.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DrawResponse {
    private Long eventId;
    private Integer totalWinners;
    private Integer firstPrizeCount;
    private Integer secondPrizeCount;
    private Integer thirdPrizeCount;
    private Integer fourthPrizeCount;
}
