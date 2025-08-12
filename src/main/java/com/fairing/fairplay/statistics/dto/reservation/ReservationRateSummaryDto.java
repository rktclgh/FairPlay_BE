package com.fairing.fairplay.statistics.dto.reservation;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class ReservationRateSummaryDto {

    private Integer totalTicket;
    private Integer totalReservation;
    private double averageReservationRate;
}
