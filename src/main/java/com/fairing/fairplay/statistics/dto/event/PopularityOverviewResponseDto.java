package com.fairing.fairplay.statistics.dto.event;

import lombok.*;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
  public class PopularityOverviewResponseDto {
    private Long totalEvents;
    private Long avgViewCount;
    private Long avgReservationCount;
    private Long avgWishlistCount;
}
