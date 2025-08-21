package com.fairing.fairplay.temp.dto.reservation;

import java.time.LocalDate;

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
public class ReservationWeeklyStatisticsDto {

    private LocalDate date;
    private Long totalQuantity;

}
