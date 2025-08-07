package com.fairing.fairplay.statistics.dto.reservation;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ReservationSummaryDto {
    private Integer totalReservations;
    private Integer totalCheckins;
    private Integer totalCancellations;
    private Integer totalNoShows;
}