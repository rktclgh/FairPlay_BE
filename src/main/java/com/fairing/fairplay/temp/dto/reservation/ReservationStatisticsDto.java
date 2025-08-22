package com.fairing.fairplay.temp.dto.reservation;

import java.math.BigDecimal;

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

    private Integer totalQuantity;
    private Integer canceledCount;
    private BigDecimal totalAmount;
    private BigDecimal averagePrice;

}
