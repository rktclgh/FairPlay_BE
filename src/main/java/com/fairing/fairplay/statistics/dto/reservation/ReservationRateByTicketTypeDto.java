package com.fairing.fairplay.statistics.dto.reservation;

import lombok.*;

@Builder
@Getter
@Setter
public class ReservationRateByTicketTypeDto {

    private String ticketType;
    private Integer stock;
    private Integer reservation;
    private double reservationRate;
}
