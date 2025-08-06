package com.fairing.fairplay.statistics.dto.hourly;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HourlyAnalysisResponseDto {
    private HourlyStatsSummaryDto summary;
    private PeakHoursSummaryDto peakHours;
    private List<HourlyDetailDataDto> hourlyDetails;
    private PatternAnalysisDto patternAnalysis;
}