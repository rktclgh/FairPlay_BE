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
public class PopularEventStatisticsDto {
    private Long averageViewCount;
    private Long averageReservationCount;
    private Long averageWishlistCount;

}
