package com.fairing.fairplay.temp.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventWeeklyStatisticsDto {

    private LocalDate date;
    private Long totalViewCount;
    private Long totalEventCount;
    private Long totalWishlistCount;

}
