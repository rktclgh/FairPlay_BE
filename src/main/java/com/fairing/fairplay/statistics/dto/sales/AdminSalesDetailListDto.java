package com.fairing.fairplay.statistics.dto.sales;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
public class AdminSalesDetailListDto {

    private BigDecimal totalSales;
    private BigDecimal reservationRevenue;
    private BigDecimal advertisingRevenue;
    private BigDecimal boothRevenue;
    private BigDecimal otherRevenue;
    private Long paymentCount;
    private BigDecimal averagePaymentAmount;
    private LocalDate statDate;

}
