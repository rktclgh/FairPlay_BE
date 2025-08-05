package com.fairing.fairplay.statistics.dto.sales;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesSummaryDto {
    private Long totalSales;
    private Integer totalCount;
    private Long paidSales;
    private Integer paidCount;
    private Long cancelledSales;
    private Integer cancelledCount;
    private Long refundedSales;
    private Integer refundedCount;
}

