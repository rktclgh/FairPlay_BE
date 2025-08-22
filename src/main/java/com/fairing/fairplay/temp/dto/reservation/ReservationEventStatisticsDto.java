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
public class ReservationEventStatisticsDto {
    private String eventName;
    private String category;
    private Integer reservationCount;
    private Integer totalAmount;

}
