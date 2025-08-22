package com.fairing.fairplay.temp.dto.reservation;

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
public class ReservationMonthlyStatisticsDto {

    private Integer weekNumber; // 주차 (1, 2, 3, 4)
    private Long totalQuantity;

}
