package com.fairing.fairplay.statistics.dto.hourly;

import lombok.*;
import java.util.List;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatternAnalysisDto {
    private String morningPattern;    // 오전 패턴 (6-12시)
    private String afternoonPattern;  // 오후 패턴 (12-18시)
    private String eveningPattern;    // 저녁 패턴 (18-24시)
    private String nightPattern;      // 새벽 패턴 (0-6시)
    private String overallTrend;
    private List<String> insights;
}