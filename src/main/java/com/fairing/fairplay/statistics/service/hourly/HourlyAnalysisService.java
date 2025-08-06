package com.fairing.fairplay.statistics.service.hourly;

import com.fairing.fairplay.statistics.dto.hourly.*;
import com.fairing.fairplay.statistics.entity.hourly.EventHourlyStatistics;
import com.fairing.fairplay.statistics.repository.hourlystats.EventHourlyStatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class HourlyAnalysisService {

    private final EventHourlyStatisticsRepository hourlyStatsRepository;
    private final double UPWARD = 1.2;
    private final double DOWNWARD = 0.8;
    private final double EVENING = 0.4;
    private final double NIGHT = 0.2;

    public HourlyAnalysisResponseDto analyzeHourlyBookings(Long eventId, LocalDate startDate, LocalDate endDate) {
        // 시간대별 통계 데이터 조회 (기간별)
        List<EventHourlyStatistics> hourlyStats = hourlyStatsRepository.findByEventIdAndDateRange(eventId, startDate, endDate);
        // 데이터가 없으면 실시간으로 계산
        if (hourlyStats.isEmpty()) {
            hourlyStats = hourlyStatsRepository.calculateWithRevenueByEventAndDateRange(eventId, startDate, endDate);
        }

        // 시간별로 집계 (여러 날짜의 같은 시간대를 합산)
        Map<Integer, EventHourlyStatistics> aggregatedStats = hourlyStats.stream()
                .collect(Collectors.toMap(
                EventHourlyStatistics::getHour,
                Function.identity(),
                        (s1, s2) -> EventHourlyStatistics.builder()
                        .eventId(eventId)
                        .hour(s1.getHour())
                .reservations(s1.getReservations() + s2.getReservations())
                .totalRevenue(s1.getTotalRevenue().add(s2.getTotalRevenue()))
                .build()
                ));
        List<EventHourlyStatistics> aggregatedList = new ArrayList<>(aggregatedStats.values());
        // 전체 합계 계산
        long totalReservations = aggregatedList.stream()
                .mapToLong(EventHourlyStatistics::getReservations)
                .sum();

        BigDecimal totalRevenue = aggregatedList.stream()
                .map(EventHourlyStatistics::getTotalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return HourlyAnalysisResponseDto.builder()
                .summary(buildSummary(aggregatedList, totalReservations, totalRevenue,startDate,endDate))
                .peakHours(buildPeakHours(aggregatedList, totalReservations))
                .hourlyDetails(buildHourlyDetails(aggregatedList, totalReservations))
                .patternAnalysis(buildPatternAnalysis(aggregatedList))
                .build();
    }

    private HourlyStatsSummaryDto buildSummary(List<EventHourlyStatistics> stats, long totalReservations, BigDecimal totalRevenue,LocalDate startDate, LocalDate endDate) {
        // startDate와 endDate를 메서드 파라미터로 추가하고
        long totalHours = ChronoUnit.HOURS.between(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        double averageHourly = stats.isEmpty() ? 0 : (double) totalReservations / totalHours;

        Integer mostActiveHour = stats.stream()
                .max((a, b) -> Long.compare(a.getReservations(), b.getReservations()))
                .map(EventHourlyStatistics::getHour)
                .orElse(0);

        return HourlyStatsSummaryDto.builder()
                .totalReservations(totalReservations)
                .totalRevenue(totalRevenue)
                .averageHourlyReservations(averageHourly)
                .mostActiveHour(mostActiveHour)
                .mostActiveHourDescription(getHourDescription(mostActiveHour))
                .build();
    }

    private PeakHoursSummaryDto buildPeakHours(List<EventHourlyStatistics> stats, long totalReservations) {
        List<PeakHourDto> topHours = stats.stream()
                .sorted((a, b) -> Long.compare(b.getReservations(), a.getReservations()))
                .limit(5)
                .map(stat -> PeakHourDto.builder()
                        .hour(stat.getHour())
                        .reservations(stat.getReservations())
                        .revenue(stat.getTotalRevenue())
                        .percentage(totalReservations > 0 ?
                                (double) stat.getReservations() / totalReservations * 100 : 0)
                        .build())
                .toList();

        String peakPeriod = determinePeakPeriod(topHours);
        double peakHourPercentage = topHours.isEmpty() ? 0 : topHours.get(0).getPercentage();

        return PeakHoursSummaryDto.builder()
                .topHours(topHours)
                .peakPeriod(peakPeriod)
                .peakHourPercentage(peakHourPercentage)
                .build();
    }

    private List<HourlyDetailDataDto> buildHourlyDetails(List<EventHourlyStatistics> stats, long totalReservations) {
        return IntStream.range(0, 24)
                .mapToObj(hour -> {
                    EventHourlyStatistics stat = stats.stream()
                            .filter(s -> s.getHour().equals(hour))
                            .findFirst()
                            .orElse(EventHourlyStatistics.builder()
                                    .hour(hour)
                                    .reservations(0L)
                                    .totalRevenue(BigDecimal.ZERO)
                                    .build());

                    double percentage = totalReservations > 0 ?
                            (double) stat.getReservations() / totalReservations * 100 : 0;

                    return HourlyDetailDataDto.builder()
                            .hour(hour)
                            .timeRange(String.format("%02d:00 - %02d:59", hour, hour))
                            .reservations(stat.getReservations())
                            .revenue(stat.getTotalRevenue())
                            .percentage(BigDecimal.valueOf(percentage)
                                    .setScale(1, RoundingMode.HALF_UP)
                                    .doubleValue())
                            .trend(determineTrend(stats, hour))
                            .description(getHourDescription(hour))
                            .build();
                })
                .toList();
    }

    private PatternAnalysisDto buildPatternAnalysis(List<EventHourlyStatistics> stats) {
        long morningTotal = getReservationsByPeriod(stats, 6, 12);
        long afternoonTotal = getReservationsByPeriod(stats, 12, 18);
        long eveningTotal = getReservationsByPeriod(stats, 18, 24);
        long nightTotal = getReservationsByPeriod(stats, 0, 6);

        List<String> insights = generateInsights(stats, morningTotal, afternoonTotal, eveningTotal, nightTotal);

        return PatternAnalysisDto.builder()
                .morningPattern(String.format("오전 (06-12시): %d건", morningTotal))
                .afternoonPattern(String.format("오후 (12-18시): %d건", afternoonTotal))
                .eveningPattern(String.format("저녁 (18-24시): %d건", eveningTotal))
                .nightPattern(String.format("새벽 (00-06시): %d건", nightTotal))
                .overallTrend(determineOverallTrend(morningTotal, afternoonTotal, eveningTotal, nightTotal))
                .insights(insights)
                .build();
    }

    private long getReservationsByPeriod(List<EventHourlyStatistics> stats, int startHour, int endHour) {
        return stats.stream()
                .filter(stat -> stat.getHour() >= startHour && stat.getHour() < endHour)
                .mapToLong(EventHourlyStatistics::getReservations)
                .sum();
    }

    private String getHourDescription(Integer hour) {
        if (hour >= 6 && hour < 9) return "출근 시간대";
        if (hour >= 9 && hour < 12) return "오전 업무 시간";
        if (hour >= 12 && hour < 14) return "점심 시간대";
        if (hour >= 14 && hour < 18) return "오후 업무 시간";
        if (hour >= 18 && hour < 21) return "퇴근/저녁 시간대";
        if (hour >= 21 && hour < 24) return "야간 시간대";
        return "새벽 시간대";
    }

    private String determinePeakPeriod(List<PeakHourDto> topHours) {
        if (topHours.isEmpty()) return "데이터 없음";

        int peakHour = topHours.get(0).getHour();
        if (peakHour >= 18 && peakHour < 21) return "저녁 시간대 (18-21시)";
        if (peakHour >= 12 && peakHour < 14) return "점심 시간대 (12-14시)";
        if (peakHour >= 21 && peakHour < 24) return "야간 시간대 (21-24시)";
        if (peakHour >= 9 && peakHour < 12) return "오전 시간대 (9-12시)";
        return "기타 시간대";
    }

    private String determineTrend(List<EventHourlyStatistics> stats, int hour) {

        // 간단한 트렌드 분석 (이전/다음 시간과 비교)
        Map<Integer, Long> hourlyReservations = stats.stream()
                .collect(Collectors.toMap(
                EventHourlyStatistics::getHour,
                EventHourlyStatistics::getReservations,
                Long::sum
                        ));

        long currentHour = hourlyReservations.getOrDefault(hour, 0L);
        long prevHour = hourlyReservations.getOrDefault(hour > 0 ? hour - 1 : 23, 0L);

        if (currentHour > prevHour * UPWARD) return "상승";
        if (currentHour < prevHour * DOWNWARD) return "하락";
        return "안정";
    }

    private String determineOverallTrend(long morning, long afternoon, long evening, long night) {
        long max = Math.max(Math.max(morning, afternoon), Math.max(evening, night));

        if (max == evening) return "저녁 시간대 집중형";
        if (max == afternoon) return "오후 시간대 집중형";
        if (max == morning) return "오전 시간대 집중형";
        return "새벽 시간대 집중형";
    }

    private List<String> generateInsights(List<EventHourlyStatistics> stats, long morning, long afternoon, long evening, long night) {
        List<String> insights = new ArrayList<>();

        long total = morning + afternoon + evening + night;
        if (total == 0) return insights;

        if (evening > total * EVENING) {
            insights.add("저녁 시간대에 예매가 집중되는 패턴입니다.");
        }

        if (night > total * NIGHT) {
            insights.add("새벽 시간대에도 상당한 예매가 발생합니다.");
        }

        if (morning > afternoon) {
            insights.add("오전 시간대가 오후보다 활발합니다.");
        }

        return insights;
    }
}
