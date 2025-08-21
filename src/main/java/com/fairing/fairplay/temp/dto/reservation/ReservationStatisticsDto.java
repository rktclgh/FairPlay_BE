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
public class ReservationStatisticsDto {

    private Long totalQuantity;
    private Long canceledCount;
    private Long totalAmount;
    private Long averagePrice;

}
