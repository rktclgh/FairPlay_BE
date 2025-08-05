package com.fairing.fairplay.statistics.dto.sales;

import lombok.*;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusSalesDto {
    private String status;
    private Long amount;
    private Double percentage;
}


