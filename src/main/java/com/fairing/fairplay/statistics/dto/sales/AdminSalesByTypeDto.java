package com.fairing.fairplay.statistics.dto.sales;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder

public class AdminSalesByTypeDto {


    private BigDecimal totalSales;
    private BigDecimal reservationRevenue;
    private BigDecimal advertisingRevenue;
    private BigDecimal boothRevenue;
    private BigDecimal otherRevenue;

    // 결제 건수
    private Long paymentCount;
    private Long reservationPaymentCount;
    private Long advertisingPaymentCount;
    private Long boothPaymentCount;
    private Long otherPaymentCount;

    //집계일
    private LocalDate statDate;

}
