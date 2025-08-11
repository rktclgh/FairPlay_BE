package com.fairing.fairplay.statistics.service.reservation;


import com.fairing.fairplay.statistics.dto.event.EventPopularityStatisticsListDto;
import com.fairing.fairplay.statistics.dto.reservation.*;
import com.fairing.fairplay.statistics.entity.reservation.EventDailyStatistics;
import com.fairing.fairplay.statistics.entity.sales.EventDailySalesStatistics;
import com.fairing.fairplay.statistics.repository.dailystats.DailyStatsCustomRepository;
import com.fairing.fairplay.statistics.repository.dailystats.EventDailyStatisticsRepository;
import com.fairing.fairplay.statistics.repository.eventstats.EventPopularityStatsCustomRepositoryImpl;
import com.fairing.fairplay.statistics.repository.salesstats.EventDailySalesStatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminReservationService {

    private final EventDailyStatisticsRepository eventDailyStatisticsRepository;
    private final EventDailySalesStatisticsRepository eventDailySalesStatisticsRepository;
    private final DailyStatsCustomRepository dailyStatsCustomRepository;

    public AdminReservationSummaryDto getReservationSummary(LocalDate start, LocalDate end) {
        // 날짜 범위 검증
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("시작일이 종료일보다 늦을 수 없습니다.");
        }

        List<EventDailyStatistics> dailyReservationList = eventDailyStatisticsRepository.findByStatDateBetweenOrderByStatDate(start,end);
        int totalReservations = dailyReservationList.stream().mapToInt(EventDailyStatistics::getReservationCount).sum();
        int totalCancellations = dailyReservationList.stream().mapToInt(EventDailyStatistics::getCancellationCount).sum();

        List<EventDailySalesStatistics> dailySalesList  =  eventDailySalesStatisticsRepository.findByStatDateBetweenOrderByStatDate(start , end);
        BigDecimal  totalSales = dailySalesList.stream().map(EventDailySalesStatistics::getTotalSales).map(BigDecimal::valueOf) // Long → BigDecimal 변환
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalCount = dailySalesList.stream()
                .mapToLong(EventDailySalesStatistics::getTotalCount)
                .sum();

        BigDecimal avgSales = totalCount > 0
                ? totalSales.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<AdminReservationStatsByCategoryDto> categoryTrend = dailyStatsCustomRepository.aggregatedReservationByCategory(start,end);

        return AdminReservationSummaryDto.builder()
                .totalReservations(totalReservations)
                .totalCancellations(totalCancellations)
                .totalSales(totalSales)
                .avgSales(avgSales)
                .categoryTrend(categoryTrend)
                .build();
    }

    public List<ReservationDailyTrendDto> dailyTrend(LocalDate start, LocalDate end){

        if (start.isAfter(end)) {
            throw new IllegalArgumentException("시작일이 종료일보다 늦을 수 없습니다.");
        }

        List<EventDailyStatistics> dailyReservationList = eventDailyStatisticsRepository.findByStatDateBetweenOrderByStatDate(start,end);

        return dailyReservationList.stream()
                .map(s -> ReservationDailyTrendDto.builder()
                        .date(s.getStatDate())
                        .reservations(s.getReservationCount())
                        .build())
                .toList();
    }

    public List<ReservationMonthlyTrendDto> monthlyTrend(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("시작일이 종료일보다 늦을 수 없습니다.");
        }

        List<EventDailyStatistics> dailyReservationList =
                eventDailyStatisticsRepository.findByStatDateBetweenOrderByStatDate(start, end);

        // 월별 그룹화
        Map<YearMonth, Long> monthlyReservations = dailyReservationList.stream()
                .collect(Collectors.groupingBy(
                        s -> YearMonth.from(s.getStatDate()), // 월별 키
                        Collectors.summingLong(EventDailyStatistics::getReservationCount)
                ));

        // DTO 변환
        return monthlyReservations.entrySet().stream()
                .map(e -> ReservationMonthlyTrendDto.builder()
                        .yearMonth(e.getKey())
                        .reservations(e.getValue())
                        .build())
                .sorted(Comparator.comparing(ReservationMonthlyTrendDto::getYearMonth))
                .toList();
    }

    public Page<AdminReservationStatsListDto> getEventsByCategory(LocalDate startDate, LocalDate endDate, String mainCategory, String subCategory, Pageable pageable) {
        if (mainCategory != null && "all".equalsIgnoreCase(mainCategory)) {
            mainCategory = "";
        }
        if (subCategory != null && "all".equalsIgnoreCase(subCategory)) {
            subCategory = "";
        }
        return dailyStatsCustomRepository.aggregatedPopularity(startDate, endDate, mainCategory, subCategory, pageable);
    }

    /**
     * 검색 기능
     */
    public List<AdminReservationStatsListDto> searchEvents(LocalDate startDate, LocalDate endDate, String keyword, String mainCategory, String subCategory) {
        if (mainCategory != null && "all".equalsIgnoreCase(mainCategory)) {
            mainCategory = "";
        }
        if (subCategory != null && "all".equalsIgnoreCase(subCategory)) {
            subCategory = "";
        }
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("검색어(keyword)는 null 또는 빈 문자열일 수 없습니다.");
        }
        return dailyStatsCustomRepository.searchEventReservationWithRank(startDate,  endDate,  keyword, mainCategory,  subCategory);
    }
}
