package com.fairing.fairplay.statistics.service.kpi;


import com.fairing.fairplay.statistics.dto.kpi.KpiSummaryDto;
import com.fairing.fairplay.statistics.dto.kpi.KpiTrendDto;
import com.fairing.fairplay.statistics.dto.kpi.KpiTrendMonthlyDto;
import com.fairing.fairplay.statistics.entity.kpi.AdminKpiStatistics;
import com.fairing.fairplay.statistics.repository.kpistats.AdminKpiStatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminKpiService {

    private final AdminKpiStatisticsRepository adminKpiStatisticsRepository;

    public KpiSummaryDto summaryKpi(LocalDate startDate, LocalDate endDate){

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("시작일/종료일은 null일 수 없습니다.");
            }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작일이 종료일보다 늦을 수 없습니다.");
            }
        List<AdminKpiStatistics> dailyKpiList = adminKpiStatisticsRepository.findByStatDateBetweenOrderByStatDate(startDate,endDate);
        if (dailyKpiList.isEmpty()) {
            return KpiSummaryDto.builder()
                    .totalEvents(0L)
                    .totalReservations(0L)
                    .totalUsers(0L)
                    .totalSales(BigDecimal.ZERO)
                    .build();
        }

        Long totalReservation = dailyKpiList.stream().mapToLong(stat -> stat.getTotalReservations() != null
                ? stat.getTotalReservations()
                : 0L).sum();

        BigDecimal totalSales = dailyKpiList.stream()
                .map(AdminKpiStatistics::getTotalSales)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long totalUsers = dailyKpiList.stream().mapToLong(stat -> stat.getTotalUsers() != null
                ? stat.getTotalUsers()
                : 0L).sum();

        Long totalEvents = dailyKpiList.stream().mapToLong(AdminKpiStatistics::getTotalEvents).sum();

        return KpiSummaryDto.builder()
                .totalEvents(totalEvents)
                .totalReservations(totalReservation)
                .totalUsers(totalUsers)
                .totalSales(totalSales)
                .build();

    }




    public List<KpiTrendDto> dailyTrend(LocalDate start, LocalDate end){

        if (start == null || end == null) {
            throw new IllegalArgumentException("시작일/종료일은 null일 수 없습니다.");
            }

        if (start.isAfter(end)) {
            throw new IllegalArgumentException("시작일이 종료일보다 늦을 수 없습니다.");
        }

        boolean shortRange = ChronoUnit.DAYS.between(start, end) <= 6;
        List<AdminKpiStatistics> dailyKpiList = shortRange
                ? adminKpiStatisticsRepository.findTop7ByStatDateBetweenOrderByStatDateDesc(start, end)
                : adminKpiStatisticsRepository.findByStatDateBetweenOrderByStatDate(start, end);


        return dailyKpiList.stream()
                .sorted(Comparator.comparing(AdminKpiStatistics::getStatDate)) // 항상 오름차순으로 응답
                .map(s -> KpiTrendDto.builder()
                        .statDate(s.getStatDate())
                        .reservations(s.getTotalReservations())
                        .sales(s.getTotalSales())
                        .build())
                .toList();
    }

    public List<KpiTrendMonthlyDto> monthlyTrend(LocalDate start, LocalDate end) {

        if (start == null || end == null) {
            throw new IllegalArgumentException("시작일/종료일은 null일 수 없습니다.");
        }

        if (start.isAfter(end)) {
            throw new IllegalArgumentException("시작일이 종료일보다 늦을 수 없습니다.");
        }

        List<AdminKpiStatistics> dailyKpiList =
                adminKpiStatisticsRepository.findByStatDateBetweenOrderByStatDate(start, end);


        // 월별 그룹화 및 DTO 변환
         return dailyKpiList.stream()
                .collect(Collectors.groupingBy(
                        s -> YearMonth.from(s.getStatDate()), // 월별 키
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    long reservations = list.stream()
                                            .mapToLong(stat -> stat.getTotalReservations() != null
                                            ? stat.getTotalReservations()
                                            : 0L)
                                            .sum();

                                    BigDecimal totalSales = list.stream()
                                            .map(AdminKpiStatistics::getTotalSales) // BigDecimal
                                            .filter(Objects::nonNull)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                                    return KpiTrendMonthlyDto.builder()
                                            .yearMonth(YearMonth.from(list.get(0).getStatDate()))
                                            .reservations(reservations)
                                            .sales(totalSales)
                                            .build();
                                }
                        )
                        ))
                 .values().stream()
                .sorted(Comparator.comparing(KpiTrendMonthlyDto::getYearMonth))
                .toList();
    }
}
