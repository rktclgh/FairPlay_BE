package com.fairing.fairplay.temp.repository.sales;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.fairing.fairplay.event.entity.QEvent;
import com.fairing.fairplay.payment.entity.QPayment;
import com.fairing.fairplay.payment.entity.QPaymentTargetType;
import com.fairing.fairplay.temp.dto.sales.AllSalesDto;
import com.fairing.fairplay.temp.dto.sales.DailySalesDto;
import com.fairing.fairplay.temp.dto.sales.TotalSalesStatisticsDto;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SalesStatisticsRepository {
    private final JPAQueryFactory queryFactory;
    private final QEvent e = QEvent.event;
    private final QPayment p = QPayment.payment;
    private final QPaymentTargetType ptt = QPaymentTargetType.paymentTargetType;
    private NumberExpression<BigDecimal> totalSales = p.amount.sum();
    private NumberExpression<BigDecimal> totalRefund = p.refundedAmount.sum();
    private NumberExpression<BigDecimal> totalRevenue = totalSales.subtract(totalRefund);

    public TotalSalesStatisticsDto getTotalSalesStatistics() {
        List<Tuple> results = queryFactory.select(
                totalRevenue,
                p.paymentId.count())
                .from(p)
                .where(p.paidAt.isNotNull()) // null 체크 추가
                .fetch();

        if (results.isEmpty() || results.get(0) == null) {
            return TotalSalesStatisticsDto.builder()
                    .totalRevenue(BigDecimal.ZERO)
                    .totalPayments(0L)
                    .build();
        }

        Tuple tuple = results.get(0);
        BigDecimal revenue = tuple.get(0, BigDecimal.class);
        Long payments = tuple.get(1, Long.class);

        return TotalSalesStatisticsDto.builder()
                .totalRevenue(revenue != null ? revenue : BigDecimal.ZERO)
                .totalPayments(payments != null ? payments : 0L)
                .build();
    }

    public List<DailySalesDto> getDailySales(LocalDate startDate, LocalDate endDate) {
        List<Tuple> results = queryFactory
                .select(
                        Expressions.stringTemplate("DATE({0})", p.paidAt),
                        ptt.paymentTargetName,
                        p.amount.sum(),
                        p.paymentId.count())
                .from(p)
                .join(p.paymentTargetType, ptt)
                .where(
                        p.refundedAmount.loe(0.1),
                        p.paidAt.isNotNull(), // NULL 체크 추가
                        startDate != null && endDate != null ? p.paidAt.between(
                                startDate.atStartOfDay(),
                                endDate.plusDays(1).atStartOfDay())
                                : startDate != null ? p.paidAt.goe(startDate.atStartOfDay())
                                        : endDate != null ? p.paidAt.lt(endDate.plusDays(1).atStartOfDay()) : null)
                .groupBy(
                        Expressions.stringTemplate("DATE({0})", p.paidAt),
                        ptt.paymentTargetName)
                .orderBy(Expressions.stringTemplate("DATE({0})", p.paidAt).asc())
                .fetch();

        // 날짜별로 결제 유형별 금액을 그룹화
        Map<LocalDate, Map<String, BigDecimal>> groupedByDate = results.stream()
                .filter(tuple -> tuple.get(0, java.sql.Date.class) != null) // null 값 필터링
                .collect(Collectors.groupingBy(
                        tuple -> tuple.get(0, java.sql.Date.class).toLocalDate(),
                        Collectors.toMap(
                                tuple -> tuple.get(1, String.class),
                                tuple -> tuple.get(2, BigDecimal.class),
                                (existing, replacement) -> existing)));

        // 날짜별로 총 결제 건수를 그룹화
        Map<LocalDate, Long> groupedCountByDate = results.stream()
                .filter(tuple -> tuple.get(0, java.sql.Date.class) != null) // null 값 필터링
                .collect(Collectors.groupingBy(
                        tuple -> tuple.get(0, java.sql.Date.class).toLocalDate(),
                        Collectors.summingLong(tuple -> tuple.get(3, Long.class))));

        // 최종 결과 생성
        return groupedByDate.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    Map<String, BigDecimal> typeAmounts = entry.getValue();
                    Long totalCount = groupedCountByDate.get(date);

                    BigDecimal reservationAmount = typeAmounts.getOrDefault("티켓", BigDecimal.ZERO);
                    BigDecimal boothAmount = typeAmounts.getOrDefault("부스", BigDecimal.ZERO);
                    BigDecimal adAmount = typeAmounts.getOrDefault("광고", BigDecimal.ZERO);
                    BigDecimal boothApplication = typeAmounts.getOrDefault("부스 신청", BigDecimal.ZERO);
                    BigDecimal bannerApplication = typeAmounts.getOrDefault("배너 신청", BigDecimal.ZERO);
                    BigDecimal totalAmount = reservationAmount.add(boothAmount)
                            .add(adAmount).add(boothApplication).add(bannerApplication);

                    return DailySalesDto.builder()
                            .date(date)
                            .reservationAmount(reservationAmount)
                            .boothAmount(boothAmount)
                            .adAmount(adAmount)
                            .boothApplication(boothApplication)
                            .bannerApplication(bannerApplication)
                            .totalAmount(totalAmount)
                            .totalCount(totalCount != null ? totalCount : 0L)
                            .build();
                })
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .toList();
    }

    public DailySalesDto getCompare() {
        List<DailySalesDto> allData = getDailySales(null, null);
        BigDecimal reservationAmount = allData.stream()
                .map(DailySalesDto::getReservationAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal boothAmount = allData.stream()
                .map(DailySalesDto::getBoothAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal adAmount = allData.stream()
                .map(DailySalesDto::getAdAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal boothApplication = allData.stream()
                .map(DailySalesDto::getBoothApplication)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal bannerApplication = allData.stream()
                .map(DailySalesDto::getBannerApplication)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAmount = allData.stream()
                .map(DailySalesDto::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Long totalCount = allData.stream()
                .map(DailySalesDto::getTotalCount)
                .reduce(0L, Long::sum);

        return DailySalesDto.builder()
                .date(LocalDate.now())
                .reservationAmount(reservationAmount)
                .boothAmount(boothAmount)
                .adAmount(adAmount)
                .boothApplication(boothApplication)
                .bannerApplication(bannerApplication)
                .totalAmount(totalAmount)
                .totalCount(totalCount)
                .build();
    }

    public Page<AllSalesDto> getAllSales(Pageable pageable) {
        // 페이징된 결과 조회
        List<Tuple> results = queryFactory
                .select(
                        e.eventId,
                        e.titleKr,
                        e.eventDetail.startDate,
                        e.eventDetail.endDate,
                        p.amount.sum(),
                        p.refundedAmount.sum().coalesce(BigDecimal.ZERO))
                .from(p)
                .join(p.event, e)
                .where(
                        e.titleKr.isNotNull(),
                        p.amount.isNotNull())
                .groupBy(e.eventId, e.titleKr, e.eventDetail.startDate, e.eventDetail.endDate)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 카운트 조회
        Long total = queryFactory
                .select(e.eventId.countDistinct())
                .from(p)
                .join(p.event, e)
                .where(
                        e.titleKr.isNotNull(),
                        p.amount.isNotNull())
                .fetchOne();

        // DTO 변환
        List<AllSalesDto> content = results.stream()
                .filter(t -> t != null && t.get(1, String.class) != null)
                .map(t -> {
                    BigDecimal totalAmount = t.get(4, BigDecimal.class);
                    BigDecimal refundAmount = t.get(5, BigDecimal.class);
                    BigDecimal netAmount = totalAmount.subtract(refundAmount != null ? refundAmount : BigDecimal.ZERO);

                    return new AllSalesDto(
                            t.get(1, String.class),
                            t.get(2, LocalDate.class),
                            t.get(3, LocalDate.class),
                            netAmount,
                            netAmount.multiply(BigDecimal.valueOf(0.08)),
                            netAmount.multiply(BigDecimal.valueOf(0.92)));
                }).toList();
        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

}