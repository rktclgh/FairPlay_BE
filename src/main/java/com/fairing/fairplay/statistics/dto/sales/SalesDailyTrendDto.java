package com.fairing.fairplay.statistics.dto.sales;

import jakarta.persistence.Column;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesDailyTrendDto {
    private LocalDate statDate;
    private Long totalSales = 0L;
    private Integer totalCount = 0;
}

