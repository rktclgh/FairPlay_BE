package com.fairing.fairplay.statistics.dto.hourly;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeakHourDto {
    private Integer hour;
    private Long reservations;
    private BigDecimal revenue;
    private Double percentage;
}