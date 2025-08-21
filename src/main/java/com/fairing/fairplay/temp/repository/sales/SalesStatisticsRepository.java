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
                p.paymentId.count(),
                totalRevenue.divide(p.paymentId.count()).coalesce(BigDecimal.ZERO))
                .from(p)
                .fetch();

        if (results.isEmpty()) {
            return new TotalSalesStatisticsDto();
        }

        Tuple tuple = results.get(0);
        BigDecimal b = tuple.get(2, Double.class) == null ? BigDecimal.ZERO
                : BigDecimal.valueOf(tuple.get(2, Double.class));
        return new TotalSalesStatisticsDto(
                tuple.get(0, BigDecimal.class),
                tuple.get(1, Long.class),
                b);
    }

    public List<DailySalesDto> getDailySales(LocalDate startDate, LocalDate endDate) {
        List<Tuple> results = queryFactory
                .select(
                        Expressions.stringTemplate("DATE({0})", p.paidAt),
                        ptt.paymentTargetName,
                        p.amount.sum(),
                        p.paymentId.count())
                .from(p)
                .leftJoin(p.paymentTargetType, ptt)
                .where(
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

        Map<LocalDate, Map<String, BigDecimal>> groupedByDate = results.stream()
                .collect(Collectors.groupingBy(
                        tuple -> tuple.get(0, java.sql.Date.class).toLocalDate(),
                        Collectors.toMap(
                                tuple -> tuple.get(1, String.class),
                                tuple -> tuple.get(2, BigDecimal.class),
                                (existing, replacement) -> existing)));

        Map<LocalDate, Long> groupedCountByDate = results.stream()
                .collect(Collectors.groupingBy(
                        tuple -> tuple.get(0, java.sql.Date.class).toLocalDate(),
                        Collectors.summingLong(tuple -> tuple.get(3, Long.class))));

        return groupedByDate.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    Map<String, BigDecimal> typeAmounts = entry.getValue();
                    Long totalCount = groupedCountByDate.get(date);

                    BigDecimal reservationAmount = typeAmounts.getOrDefault("예약", BigDecimal.ZERO);
                    BigDecimal boothAmount = typeAmounts.getOrDefault("부스", BigDecimal.ZERO);
                    BigDecimal adAmount = typeAmounts.getOrDefault("광고", BigDecimal.ZERO);
                    BigDecimal etcAmount = typeAmounts.entrySet().stream()
                            .filter(e -> !List.of("예약", "부스", "광고").contains(e.getKey()))
                            .map(Map.Entry::getValue)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal totalAmount = reservationAmount.add(boothAmount)
                            .add(adAmount).add(etcAmount);

                    return DailySalesDto.builder()
                            .date(date)
                            .reservationAmount(reservationAmount)
                            .boothAmount(boothAmount)
                            .adAmount(adAmount)
                            .etcAmount(etcAmount)
                            .totalAmount(totalAmount)
                            .totalCount(totalCount)
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
        BigDecimal etcAmount = allData.stream()
                .map(DailySalesDto::getEtcAmount)
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
                .etcAmount(etcAmount)
                .totalAmount(totalAmount)
                .totalCount(totalCount)
                .build();
    }

    public Page<AllSalesDto> getAllSales(Pageable pageable) {

        List<Tuple> results = queryFactory
                .select(
                        p.event.titleKr,
                        p.event.eventDetail.startDate,
                        p.event.eventDetail.endDate,
                        p.amount.sum())
                .from(p)
                .leftJoin(e).on(p.event.eventId.eq(e.eventId))
                .groupBy(p.event.titleKr, p.event.eventDetail.startDate, p.event.eventDetail.endDate)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(p.event.titleKr.countDistinct())
                .from(p)
                .leftJoin(e).on(p.event.eventId.eq(e.eventId))
                .fetchOne();

        List<AllSalesDto> content = results.stream().map(t -> {
            BigDecimal totalAmount = t.get(3, BigDecimal.class);
            return new AllSalesDto(
                    t.get(0, String.class),
                    t.get(1, LocalDate.class),
                    t.get(2, LocalDate.class),
                    totalAmount,
                    totalAmount.multiply(BigDecimal.valueOf(0.08)),
                    totalAmount.multiply(BigDecimal.valueOf(0.92)));
        }).toList();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

}
