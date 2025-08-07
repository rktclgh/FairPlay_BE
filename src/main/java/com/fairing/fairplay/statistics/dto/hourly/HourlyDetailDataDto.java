package com.fairing.fairplay.statistics.dto.hourly;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HourlyDetailDataDto {
    private Integer hour;
    private String timeRange;
    private Long reservations;
    private BigDecimal revenue;
    private Double percentage;
    private String trend;
    private String description;
}
