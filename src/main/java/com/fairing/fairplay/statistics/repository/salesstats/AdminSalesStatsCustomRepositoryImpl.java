package com.fairing.fairplay.statistics.repository.salesstats;


import com.fairing.fairplay.payment.entity.QPayment;
import com.fairing.fairplay.payment.entity.QPaymentTargetType;
import com.fairing.fairplay.statistics.entity.sales.AdminSalesStatistics;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.DateExpression;
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
public class AdminSalesStatsCustomRepositoryImpl implements AdminSalesStatsCustomRepository{

    private final JPAQueryFactory queryFactory;

    @Override
    public AdminSalesStatistics calculate(LocalDate targetDate) {

        QPayment payment = QPayment.payment;
        QPaymentTargetType paymentTargetType = QPaymentTargetType.paymentTargetType;

        DateExpression<LocalDate> statDate =
                Expressions.dateTemplate(LocalDate.class, "DATE({0})", payment.paidAt);

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
                        statDate,
                        totalRevenue,
                        reservationRevenue,
                        advertisingRevenue,
                        boothRevenue,
                        otherRevenue,
                        paymentCount,
                        avgPaymentAmount
                )
                .from(payment)
                .join(payment.paymentTargetType, paymentTargetType)
                .where(payment.paidAt.between(targetDate.atStartOfDay(), targetDate.atTime(23, 59, 59)))
                .groupBy(statDate)
                .orderBy(statDate.asc())
                .fetchOne();


        if (result == null) {
            return AdminSalesStatistics.builder()
                    .statDate(targetDate)
                    .totalSales(BigDecimal.ZERO)
                    .reservationRevenue(BigDecimal.ZERO)
                    .advertisingRevenue(BigDecimal.ZERO)
                    .boothRevenue(BigDecimal.ZERO)
                    .otherRevenue(BigDecimal.ZERO)
                    .paymentCount(0L)
                    .averagePaymentAmount(BigDecimal.ZERO)
                    .createdAt(LocalDateTime.now())
                    .build();
        }
        return AdminSalesStatistics.builder()
                .statDate(result.get(statDate))
                .totalSales(result.get(totalRevenue))
                .reservationRevenue(result.get(reservationRevenue))
                .advertisingRevenue(result.get(advertisingRevenue))
                .boothRevenue(result.get(boothRevenue))
                .otherRevenue(result.get(otherRevenue))
                .paymentCount(result.get(paymentCount))
                .averagePaymentAmount(result.get(avgPaymentAmount))
                .createdAt(LocalDateTime.now())
                .build();
    }




}
