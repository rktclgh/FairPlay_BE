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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminKpiService {

    private final AdminKpiStatisticsRepository adminKpiStatisticsRepository;

    public KpiSummaryDto summaryKpi(LocalDate startDate, LocalDate endDate){

        List<AdminKpiStatistics> dailyKpiList = adminKpiStatisticsRepository.findByStatDateBetweenOrderByStatDate(startDate,endDate);

        Long totalRervation = dailyKpiList.stream().mapToLong(AdminKpiStatistics::getTotalReservations).sum();
        BigDecimal totalSales = dailyKpiList.stream().map((AdminKpiStatistics::getTotalSales))// Long → BigDecimal 변환
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Long totalUsers = dailyKpiList.stream().mapToLong(AdminKpiStatistics::getTotalUsers).sum();
        Long totalEvents = dailyKpiList.stream().mapToLong(AdminKpiStatistics::getTotalEvents).sum();

        return KpiSummaryDto.builder()
                .totalEvents(totalEvents)
                .totalReservations(totalRervation)
                .totalUsers(totalUsers)
                .totalSales(totalSales)
                .build();

    }




    public List<KpiTrendDto> dailyTrend(LocalDate start, LocalDate end){

        if (start.isAfter(end)) {
            throw new IllegalArgumentException("시작일이 종료일보다 늦을 수 없습니다.");
        }

        List<AdminKpiStatistics> dailyKpiList = adminKpiStatisticsRepository.findTop7ByStatDateBetweenOrderByStatDateDesc(start,end);

        return dailyKpiList.stream()
                .map(s -> KpiTrendDto.builder()
                        .statDate(s.getStatDate())
                        .reservation(s.getTotalReservations())
                        .sales(s.getTotalSales())
                        .build())
                .toList();
    }

    public List<KpiTrendMonthlyDto> monthlyTrend(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("시작일이 종료일보다 늦을 수 없습니다.");
        }

        List<AdminKpiStatistics> dailyKpiList =
                adminKpiStatisticsRepository.findByStatDateBetweenOrderByStatDate(start, end);

        // 월별 그룹화
        Map<YearMonth, KpiTrendMonthlyDto> monthlyTrendList = dailyKpiList.stream()
                .collect(Collectors.groupingBy(
                        s -> YearMonth.from(s.getStatDate()), // 월별 키
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    long reservations = list.stream()
                                            .mapToLong(AdminKpiStatistics::getTotalReservations)
                                            .sum();

                                    BigDecimal totalSales = list.stream()
                                            .map(AdminKpiStatistics::getTotalSales) // BigDecimal
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                                    return KpiTrendMonthlyDto.builder()
                                            .yearMonth(YearMonth.from(list.get(0).getStatDate()))
                                            .reservations(reservations)
                                            .sales(totalSales)
                                            .build();
                                }
                        )
                ));

        // DTO 변환
        return monthlyTrendList.entrySet().stream()
                .map(e -> KpiTrendMonthlyDto.builder()
                        .yearMonth(e.getKey())
                        .reservations(e.getValue().getReservations())
                        .sales(e.getValue().getSales())
                        .build())
                .sorted(Comparator.comparing(KpiTrendMonthlyDto::getYearMonth))
                .toList();
    }
}
