package com.fairing.fairplay.statistics.dto.hourly;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeakHoursSummaryDto {
    private List<PeakHourDto> topHours;
    private String peakPeriod;
    private Double peakHourPercentage;
}