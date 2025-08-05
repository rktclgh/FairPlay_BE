package com.fairing.fairplay.statistics.dto.reservation;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class ReservationDailyTrendDto {
    private LocalDate date;
    private Integer reservations;
}
