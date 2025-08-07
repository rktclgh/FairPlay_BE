package com.fairing.fairplay.statistics.dto.sales;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionSalesDto {
    private LocalDate statDate;
    private String ticketName;
    private Long unitPrice;
    private Integer quantity;
    private Long salesAmount;
    private String paymentStatusCode;
}

