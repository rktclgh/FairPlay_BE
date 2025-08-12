package com.fairing.fairplay.statistics.dto.reservation;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Builder
@Getter
@Setter
public class ReservationRateBySessionDto {
    private Long scheduleId;
    private LocalDate date;
    private LocalTime startTime;
    private Integer stock;
    private Integer reservation;
    private double reservationRate;
}
