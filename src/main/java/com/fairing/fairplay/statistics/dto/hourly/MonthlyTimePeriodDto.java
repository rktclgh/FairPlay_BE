package com.fairing.fairplay.statistics.dto.hourly;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyTimePeriodDto {
    private String month;
    private Long morning;
    private Long afternoon;
    private Long evening;
}