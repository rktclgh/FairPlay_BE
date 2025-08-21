package com.fairing.fairplay.temp.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventMonthlyStatisticsDto {

    private Integer weekNumber; // 주차 (1, 2, 3, 4, 5)
    private Long totalViewCount;
    private Long totalEventCount;
    private Long totalWishlistCount;

}
