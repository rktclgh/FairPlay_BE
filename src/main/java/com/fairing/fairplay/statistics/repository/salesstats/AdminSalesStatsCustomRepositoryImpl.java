package com.fairing.fairplay.statistics.repository.salesstats;

import com.fairing.fairplay.payment.entity.QPayment;
import com.fairing.fairplay.payment.entity.QPaymentTargetType;
import com.fairing.fairplay.statistics.entity.sales.AdminSalesStatistics;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class AdminSalesStatsCustomRepositoryImpl implements AdminSalesStatsCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public AdminSalesStatistics calculate(LocalDate targetDate) {

        QPayment payment = QPayment.payment;
        QPaymentTargetType paymentTargetType = QPaymentTargetType.paymentTargetType;

        NumberExpression<BigDecimal> reservationRevenue =
                Expressions.numberTemplate(BigDecimal.class,
                        "SUM(CASE WHEN {0} = 'RESERVATION' THEN {1} ELSE 0 END)",
                        paymentTargetType.paymentTargetCode,
                        payment.amount
                );

        NumberExpression<BigDecimal> advertisingRevenue =
                Expressions.numberTemplate(BigDecimal.class,
                        "SUM(CASE WHEN {0} = 'AD' THEN {1} ELSE 0 END)",
                        paymentTargetType.paymentTargetCode,
                        payment.amount
                );

        NumberExpression<BigDecimal> boothRevenue =
                Expressions.numberTemplate(BigDecimal.class,
                        "SUM(CASE WHEN {0} = 'BOOTH' THEN {1} ELSE 0 END)",
                        paymentTargetType.paymentTargetCode,
                        payment.amount
                );

        NumberExpression<BigDecimal> otherRevenue =
                Expressions.numberTemplate(BigDecimal.class,
                        "SUM(CASE WHEN {0} NOT IN ('RESERVATION','AD','BOOTH') THEN {1} ELSE 0 END)",
                        paymentTargetType.paymentTargetCode,
                        payment.amount
                );

        NumberExpression<BigDecimal> totalRevenue =
                Expressions.numberTemplate(BigDecimal.class,
                        "SUM({0})",
                        payment.amount
                );

        NumberExpression<Long> paymentCount =
                Expressions.numberTemplate(Long.class,
                        "COUNT(*)"
                );

        NumberExpression<Long> reservationPaymentCount =
                Expressions.numberTemplate(Long.class,
                        "SUM(CASE WHEN {0} = {1} THEN 1 ELSE 0 END)",
                        payment.paymentTargetType.paymentTargetCode,  // FK를 통해 결제 타입 코드 접근
                        "RESERVATION"                                // 원하는 타입 코드
                );

        NumberExpression<Long> boothPaymentCount =
                Expressions.numberTemplate(Long.class,
                        "SUM(CASE WHEN {0} = {1} THEN 1 ELSE 0 END)",
                        payment.paymentTargetType.paymentTargetCode,  // FK를 통해 결제 타입 코드 접근
                        "BOOTH"                                // 원하는 타입 코드
                );

        NumberExpression<Long> advertisingPaymentCount =
                Expressions.numberTemplate(Long.class,
                        "SUM(CASE WHEN {0} = {1} THEN 1 ELSE 0 END)",
                        payment.paymentTargetType.paymentTargetCode,  // FK를 통해 결제 타입 코드 접근
                        "AD"                                // 원하는 타입 코드
                );

        NumberExpression<Long> otherPaymentCount =
                Expressions.numberTemplate(Long.class,
                        "SUM(CASE WHEN {0} NOT IN ('RESERVATION','AD','BOOTH') THEN 1 ELSE 0 END)",
                        payment.paymentTargetType.paymentTargetCode  // FK를 통해 결제 타입 코드 접근

                );

        NumberExpression<BigDecimal> avgPaymentAmount =
                Expressions.numberTemplate(BigDecimal.class,
                        "ROUND(AVG({0}), 2)",
                        payment.amount
                );


        Tuple result = queryFactory
                .select(
                        totalRevenue,
                        reservationRevenue,
                        advertisingRevenue,
                        boothRevenue,
                        otherRevenue,
                        paymentCount,
                        reservationPaymentCount,
                        advertisingPaymentCount,
                        boothPaymentCount,
                        otherPaymentCount,
                        avgPaymentAmount
                )
                .from(payment)
                .join(payment.paymentTargetType, paymentTargetType)
                .where(payment.paidAt.goe(targetDate.atStartOfDay())
                        .and(payment.paidAt.lt(targetDate.plusDays(1).atStartOfDay())))
                .fetchOne();


        if (result == null) {
            return buildStatistics(targetDate, null, null, null, null, null, null, null, null, null, null, null);
        }

        return buildStatistics(
                targetDate,
                result.get(totalRevenue),
                result.get(reservationRevenue),
                result.get(advertisingRevenue),
                result.get(boothRevenue),
                result.get(otherRevenue),
                result.get(paymentCount),
                result.get(reservationPaymentCount),
                result.get(advertisingPaymentCount),
                result.get(boothPaymentCount),
                result.get(otherPaymentCount),
                result.get(avgPaymentAmount));
    }

    static AdminSalesStatistics buildStatistics(
            LocalDate targetDate,
            BigDecimal totalRevenue,
            BigDecimal reservationRevenue,
            BigDecimal advertisingRevenue,
            BigDecimal boothRevenue,
            BigDecimal otherRevenue,
            Long paymentCount,
            Long reservationPaymentCount,
            Long advertisingPaymentCount,
            Long boothPaymentCount,
            Long otherPaymentCount,
            BigDecimal averagePaymentAmount) {
        return AdminSalesStatistics.builder()
                .statDate(targetDate)
                .totalSales(zeroIfNull(totalRevenue))
                .reservationRevenue(zeroIfNull(reservationRevenue))
                .advertisingRevenue(zeroIfNull(advertisingRevenue))
                .boothRevenue(zeroIfNull(boothRevenue))
                .otherRevenue(zeroIfNull(otherRevenue))
                .paymentCount(zeroIfNull(paymentCount))
                .reservationPaymentCount(zeroIfNull(reservationPaymentCount))
                .advertisingPaymentCount(zeroIfNull(advertisingPaymentCount))
                .boothPaymentCount(zeroIfNull(boothPaymentCount))
                .otherPaymentCount(zeroIfNull(otherPaymentCount))
                .averagePaymentAmount(zeroIfNull(averagePaymentAmount))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static Long zeroIfNull(Long value) {
        return value != null ? value : 0L;
    }

}
