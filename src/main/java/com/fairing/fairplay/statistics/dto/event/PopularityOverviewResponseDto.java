package com.fairing.fairplay.statistics.dto.event;

import lombok.*;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
  public class PopularityOverviewResponseDto {
    private Long totalEvents;
  /**
   * Math.round 결과로 반올림된 평균 조회 수
   */
  private Long avgViewCount;

  /**
   * Math.round 결과로 반올림된 평균 예약 수
   */
  private Long avgReservationCount;

  /**
   * Math.round 결과로 반올림된 평균 찜 수
   */
  private Long avgWishlistCount;
}
