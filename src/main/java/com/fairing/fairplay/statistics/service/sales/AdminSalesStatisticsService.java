package com.fairing.fairplay.statistics.service.sales;

import com.fairing.fairplay.statistics.dto.sales.*;
import com.fairing.fairplay.statistics.entity.sales.AdminSalesStatistics;
import com.fairing.fairplay.statistics.repository.salesstats.AdminSalesStatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminSalesStatisticsService {

    private final AdminSalesStatisticsRepository adminSalesStatisticsRepositorys;
    private final int PAGE_SIZE = 10;
    public AdminSalesSummaryDto summarySales(LocalDate startDate, LocalDate endDate, String type){

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("시작일/종료일은 null일 수 없습니다.");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작일이 종료일보다 늦을 수 없습니다.");
        }

        List<AdminSalesStatistics> dailySalesList = adminSalesStatisticsRepositorys.findByStatDateBetweenOrderByStatDate(startDate,endDate);

        if (dailySalesList.isEmpty()) {
            return AdminSalesSummaryDto.builder()
                    .averageDailyPaymentAmount(BigDecimal.ZERO)
                    .totalSales(BigDecimal.ZERO)
                    .paymentCount(0L)
                    .build();
        }



        // 총 매출
        BigDecimal totalSales = dailySalesList.stream()
                .map(AdminSalesStatistics::getTotalSales)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 타입별 매출
        BigDecimal reservationRevenue = dailySalesList.stream()
                .map(AdminSalesStatistics::getReservationRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal advertisingRevenue = dailySalesList.stream()
                .map(AdminSalesStatistics::getAdvertisingRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal boothRevenue = dailySalesList.stream()
                .map(AdminSalesStatistics::getBoothRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal otherRevenue = dailySalesList.stream()
                .map(AdminSalesStatistics::getOtherRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 총 결제 건수
        Long totalPaymentCount = dailySalesList.stream()
                .map(AdminSalesStatistics::getPaymentCount)
                .filter(Objects::nonNull)
                .reduce(0L, Long::sum);

        long reservationPaymentCount = dailySalesList.stream()
                .map(AdminSalesStatistics::getReservationPaymentCount) // Long
                .map(count -> count != null ? count : 0L) // null이면 0
                .mapToLong(Long::longValue)
                .sum();


        long advertisingPaymentCount = dailySalesList.stream()
                .map(AdminSalesStatistics::getAdvertisingPaymentCount) // Long
                .map(count -> count != null ? count : 0L) // null이면 0
                .mapToLong(Long::longValue)
                .sum();

        long boothPaymentCount = dailySalesList.stream()
                .map(AdminSalesStatistics::getBoothPaymentCount) // Long
                .map(count -> count != null ? count : 0L) // null이면 0
                .mapToLong(Long::longValue)
                .sum();

        long otherPaymentCount = dailySalesList.stream()
                .map(AdminSalesStatistics::getOtherPaymentCount) // Long
                .map(count -> count != null ? count : 0L) // null이면 0
                .mapToLong(Long::longValue)
                .sum();


        Long count  = switch (type){
            case "AD" -> advertisingPaymentCount;
            case "BOOTH" -> boothPaymentCount;
            case "RESERVATION" -> reservationPaymentCount;
            case "OTHER" -> otherPaymentCount;
            default -> totalPaymentCount;
        };

        BigDecimal sales = switch (type){
            case "AD" -> advertisingRevenue;
            case "BOOTH" -> boothRevenue;
            case "RESERVATION" -> reservationRevenue;
            case "OTHER" -> otherRevenue;
            default -> totalSales;
        };

        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if(sales == null){
            sales = BigDecimal.ZERO;
        }
        BigDecimal avgSales = sales.divide(BigDecimal.valueOf(daysBetween), 2, RoundingMode.HALF_UP);


        return AdminSalesSummaryDto.builder()
                .totalSales(sales)
                .paymentCount(count)
                .averageDailyPaymentAmount(avgSales)
                .build();
    }


    public List<AdminSalesByTypeDto> salesTrendByType(LocalDate startDate, LocalDate endDate){

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("시작일/종료일은 null일 수 없습니다.");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작일이 종료일보다 늦을 수 없습니다.");
        }

        List<AdminSalesStatistics> dailySalesList = adminSalesStatisticsRepositorys.findByStatDateBetweenOrderByStatDate(startDate,endDate);

        if (dailySalesList.isEmpty()) {
            return List.of();

        }

        return  dailySalesList.stream().map(r ->
                AdminSalesByTypeDto.builder()
                .totalSales(r.getTotalSales())
                .reservationRevenue(r.getReservationRevenue())
                .advertisingRevenue(r.getAdvertisingRevenue())
                .boothRevenue(r.getBoothRevenue())
                .otherRevenue(r.getOtherRevenue())
                .paymentCount(r.getPaymentCount())
                .reservationPaymentCount(r.getReservationPaymentCount())
                .advertisingPaymentCount(r.getAdvertisingPaymentCount())
                .boothPaymentCount(r.getBoothPaymentCount())
                .otherPaymentCount(r.getOtherPaymentCount())
                .build())
                .toList();

    }

    public List<AdminSalesDailyTrendDto> salesDailyTrend(LocalDate startDate, LocalDate endDate, String type){
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("시작일/종료일은 null일 수 없습니다.");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작일이 종료일보다 늦을 수 없습니다.");
        }

        List<AdminSalesStatistics> dailySalesList = adminSalesStatisticsRepositorys.findByStatDateBetweenOrderByStatDate(startDate,endDate);

        if (dailySalesList.isEmpty()) {
            return List.of();
        }

        List<AdminSalesDailyTrendDto> trend = switch (type){
            case "AD" -> dailySalesList.stream().map(r->
                    AdminSalesDailyTrendDto.builder()
                            .statDate(r.getStatDate())
                            .totalSales(r.getAdvertisingRevenue())
                            .paymentCount(r.getAdvertisingPaymentCount())
                            .build())
                    .toList();
            case "BOOTH" -> dailySalesList.stream().map(r->
                            AdminSalesDailyTrendDto.builder()
                                    .statDate(r.getStatDate())
                                    .totalSales(r.getBoothRevenue())
                                    .paymentCount(r.getBoothPaymentCount())
                                    .build())
                    .toList();
            case "RESERVATION" -> dailySalesList.stream().map(r->
                            AdminSalesDailyTrendDto.builder()
                                    .statDate(r.getStatDate())
                                    .totalSales(r.getReservationRevenue())
                                    .paymentCount(r.getReservationPaymentCount())
                                    .build())
                    .toList();
            case "OTHER" -> dailySalesList.stream().map(r->
                            AdminSalesDailyTrendDto.builder()
                                    .statDate(r.getStatDate())
                                    .totalSales(r.getOtherRevenue())
                                    .paymentCount(r.getOtherPaymentCount())
                                    .build())
                    .toList();
            default -> dailySalesList.stream().map(r->
                            AdminSalesDailyTrendDto.builder()
                                    .statDate(r.getStatDate())
                                    .totalSales(r.getTotalSales())
                                    .paymentCount(r.getPaymentCount())
                                    .build())
                    .toList();
        };

        return trend;
    }


    public List<AdminSalesMonthlyTrendDto> salesMonthlyTrend(LocalDate startDate, LocalDate endDate, String type){
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("시작일/종료일은 null일 수 없습니다.");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작일이 종료일보다 늦을 수 없습니다.");
        }

        List<AdminSalesStatistics> dailySalesList = adminSalesStatisticsRepositorys.findByStatDateBetweenOrderByStatDate(startDate,endDate);

        if (dailySalesList.isEmpty()) {
            return List.of();
        }

        List<AdminSalesMonthlyTrendDto> trend = switch (type){
            case "AD" -> dailySalesList.stream()
                    .collect(Collectors.groupingBy(
                            s -> YearMonth.from(s.getStatDate())
                    ))
                    .entrySet()
                    .stream()
                    .map(entry -> {
                        YearMonth month = entry.getKey();
                        List<AdminSalesStatistics> list = entry.getValue();

                        BigDecimal totalSales = list.stream()
                                .map(AdminSalesStatistics::getAdvertisingRevenue)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        long totalPayments = list.stream()
                                .mapToLong(AdminSalesStatistics::getAdvertisingPaymentCount)
                                .sum();

                        return AdminSalesMonthlyTrendDto.builder()
                                .statDate(month)
                                .totalSales(totalSales)
                                .paymentCount(totalPayments)
                                .build();
                    })
                    .sorted(Comparator.comparing(AdminSalesMonthlyTrendDto::getStatDate)) // 월 기준 정렬
                    .toList();

            case "BOOTH" -> dailySalesList.stream()
                    .collect(Collectors.groupingBy(
                            s -> YearMonth.from(s.getStatDate())
                    ))
                    .entrySet()
                    .stream()
                    .map(entry -> {
                        YearMonth month = entry.getKey();
                        List<AdminSalesStatistics> list = entry.getValue();

                        BigDecimal totalSales = list.stream()
                                .map(AdminSalesStatistics::getBoothRevenue)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        long totalPayments = list.stream()
                                .mapToLong(AdminSalesStatistics::getBoothPaymentCount)
                                .sum();

                        return AdminSalesMonthlyTrendDto.builder()
                                .statDate(month)
                                .totalSales(totalSales)
                                .paymentCount(totalPayments)
                                .build();
                    })
                    .sorted(Comparator.comparing(AdminSalesMonthlyTrendDto::getStatDate)) // 월 기준 정렬
                    .toList();
            case "RESERVATION" -> dailySalesList.stream()
                    .collect(Collectors.groupingBy(
                            s -> YearMonth.from(s.getStatDate())
                    ))
                    .entrySet()
                    .stream()
                    .map(entry -> {
                        YearMonth month = entry.getKey();
                        List<AdminSalesStatistics> list = entry.getValue();

                        BigDecimal totalSales = list.stream()
                                .map(AdminSalesStatistics::getReservationRevenue)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        long totalPayments = list.stream()
                                .mapToLong(AdminSalesStatistics::getReservationPaymentCount)
                                .sum();

                        return AdminSalesMonthlyTrendDto.builder()
                                .statDate(month)
                                .totalSales(totalSales)
                                .paymentCount(totalPayments)
                                .build();
                    })
                    .sorted(Comparator.comparing(AdminSalesMonthlyTrendDto::getStatDate)) // 월 기준 정렬
                    .toList();

            case "OTHER" -> dailySalesList.stream()
                    .collect(Collectors.groupingBy(
                            s -> YearMonth.from(s.getStatDate())
                    ))
                    .entrySet()
                    .stream()
                    .map(entry -> {
                        YearMonth month = entry.getKey();
                        List<AdminSalesStatistics> list = entry.getValue();

                        BigDecimal totalSales = list.stream()
                                .map(AdminSalesStatistics::getOtherRevenue)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        long totalPayments = list.stream()
                                .mapToLong(AdminSalesStatistics::getOtherPaymentCount)
                                .sum();

                        return AdminSalesMonthlyTrendDto.builder()
                                .statDate(month)
                                .totalSales(totalSales)
                                .paymentCount(totalPayments)
                                .build();
                    })
                    .sorted(Comparator.comparing(AdminSalesMonthlyTrendDto::getStatDate)) // 월 기준 정렬
                    .toList();
            default -> dailySalesList.stream()
                    .collect(Collectors.groupingBy(
                            s -> YearMonth.from(s.getStatDate())
                    ))
                    .entrySet()
                    .stream()
                    .map(entry -> {
                        YearMonth month = entry.getKey();
                        List<AdminSalesStatistics> list = entry.getValue();

                        BigDecimal totalSales = list.stream()
                                .map(AdminSalesStatistics::getTotalSales)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        long totalPayments = list.stream()
                                .mapToLong(AdminSalesStatistics::getPaymentCount)
                                .sum();

                        return AdminSalesMonthlyTrendDto.builder()
                                .statDate(month)
                                .totalSales(totalSales)
                                .paymentCount(totalPayments)
                                .build();
                    })
                    .sorted(Comparator.comparing(AdminSalesMonthlyTrendDto::getStatDate)) // 월 기준 정렬
                    .toList();

        };

        return trend;
    }

    public Page<AdminSalesDetailListDto> salesDetailList(LocalDate startDate, LocalDate endDate, int size){

        Pageable pageable = PageRequest.of(PAGE_SIZE, size);

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("시작일/종료일은 null일 수 없습니다.");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작일이 종료일보다 늦을 수 없습니다.");
        }

        Page<AdminSalesStatistics> dailySalesList = adminSalesStatisticsRepositorys.findByStatDateBetweenOrderByStatDate(startDate,endDate,pageable);

        if (dailySalesList.isEmpty()) {
            return Page.empty();
        }

        return dailySalesList.map(r->
                AdminSalesDetailListDto.builder()
                        .totalSales(r.getTotalSales())
                        .advertisingRevenue(r.getAdvertisingRevenue())
                        .reservationRevenue(r.getReservationRevenue())
                        .boothRevenue(r.getBoothRevenue())
                        .otherRevenue(r.getOtherRevenue())
                        .paymentCount(r.getPaymentCount())
                        .averagePaymentAmount(r.getAveragePaymentAmount())
                        .statDate(r.getStatDate())
                        .build());


    }

}
