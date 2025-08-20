package com.fairing.fairplay.statistics.repository.salesstats;

import com.fairing.fairplay.statistics.dto.sales.PaymentStatusSalesDto;
import com.fairing.fairplay.statistics.dto.sales.SalesSummaryDto;
import com.fairing.fairplay.statistics.dto.sales.SalesDailyTrendDto;
import com.fairing.fairplay.statistics.dto.sales.SessionSalesDto;
import com.fairing.fairplay.statistics.entity.sales.QEventDailySalesStatistics;
import com.fairing.fairplay.statistics.entity.sales.QEventSessionSalesStatistics;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SalesStatisticsCustomRepositoryImpl implements SalesStatisticsCustomRepository {

    private final JPAQueryFactory queryFactory;
    QEventDailySalesStatistics daily = QEventDailySalesStatistics.eventDailySalesStatistics;
    QEventSessionSalesStatistics session = QEventSessionSalesStatistics.eventSessionSalesStatistics;

    @Override
    public SalesSummaryDto getSalesSummary(Long eventId, LocalDate start, LocalDate end) {
        Tuple result = queryFactory
                .select(
                        daily.totalSales.sum(),
                        daily.totalCount.sum(),
                        daily.paidSales.sum(),
                        daily.paidCount.sum(),
                        daily.cancelledSales.sum(),
                        daily.cancelledCount.sum(),
                        daily.refundedSales.sum(),
                        daily.refundedCount.sum()
                )
                .from(daily)
                .where(daily.eventId.eq(eventId)
                        .and(daily.statDate.between(start, end)))
                .fetchOne();

        return SalesSummaryDto.builder()
                .totalSales(result.get(daily.totalSales.sum()))
                .totalCount(result.get(daily.totalCount.sum()))
                .paidSales(result.get(daily.paidSales.sum()))
                .paidCount(result.get(daily.paidCount.sum()))
                .cancelledSales(result.get(daily.cancelledSales.sum()))
                .cancelledCount(result.get(daily.cancelledCount.sum()))
                .refundedSales(result.get(daily.refundedSales.sum()))
                .refundedCount(result.get(daily.refundedCount.sum()))
                .build();
    }
    @Override
    public List<SalesDailyTrendDto> getSalesDailyTrend(Long eventId, LocalDate start, LocalDate end) {
        List<Tuple> results = queryFactory
                .select(
                        daily.paidSales,
                        daily.paidCount,
                        daily.statDate
                )
                .from(daily)
                .where(daily.eventId.eq(eventId)
                        .and(daily.statDate.between(start, end)))
                .fetch();

        return results.stream()
                .map(r -> SalesDailyTrendDto.builder()
                        .totalSales(r.get(daily.paidSales) != null ? r.get(daily.totalSales) : 0L)
                        .totalCount(r.get(daily.paidCount) != null ? r.get(daily.totalCount) : 0)
                        .statDate(r.get(daily.statDate))
                        .build())
                .toList();
    }

    @Override
    public List<PaymentStatusSalesDto> getSalesByPaymentStatus(Long eventId, LocalDate start, LocalDate end) {
        List<Tuple> results = queryFactory
                .select(session.paymentStatusCode, session.salesAmount.sum())
                .from(session)
                .where(session.eventId.eq(eventId)
                        .and(session.statDate.between(start, end)))
                .groupBy(session.paymentStatusCode)
                .fetch();

        long totalSales = results.stream().mapToLong(r -> r.get(session.salesAmount.sum())).sum();

        return results.stream()
                .map(r -> PaymentStatusSalesDto.builder()
                        .status(r.get(session.paymentStatusCode))
                        .amount(r.get(session.salesAmount.sum()) != null ? r.get(session.salesAmount.sum()) : 0L)
                        .percentage(totalSales > 0
                                ? ((r.get(session.salesAmount.sum()) != null ? r.get(session.salesAmount.sum()) : 0L) * 100.0 / totalSales)
                                : 0.0)
                        .build())
                .toList();
    }

    @Override
    public List<SessionSalesDto> getSessionSales(Long eventId, LocalDate start, LocalDate end) {
        return queryFactory
                .select(session.statDate, session.ticketName, session.unitPrice,
                        session.quantity.sum(), session.salesAmount.sum(), session.paymentStatusCode)
                .from(session)
                .where(session.eventId.eq(eventId)
                        .and(session.statDate.between(start, end)))
                .groupBy(session.statDate, session.ticketName, session.unitPrice, session.paymentStatusCode)
                .orderBy(session.statDate.asc())
                .fetch()
                .stream()
                .map(r -> SessionSalesDto.builder()
                        .statDate(r.get(session.statDate))
                        .ticketName(r.get(session.ticketName))
                        .unitPrice(r.get(session.unitPrice))
                        .quantity(r.get(session.quantity.sum()))
                        .salesAmount(r.get(session.salesAmount.sum()))
                        .paymentStatusCode(r.get(session.paymentStatusCode))
                        .build())
                .toList();
    }
}

