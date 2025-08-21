package com.fairing.fairplay.statistics.dto.hourly;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayOfWeekSummaryDto {
    private String day;
    private Long bookings;
    private BigDecimal revenue;
    private Double percentage;
}