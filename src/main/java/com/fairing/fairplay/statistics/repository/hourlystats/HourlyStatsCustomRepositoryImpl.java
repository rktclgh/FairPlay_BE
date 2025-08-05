package com.fairing.fairplay.statistics.repository.hourlystats;

import com.fairing.fairplay.statistics.entity.hourly.EventHourlyStatistics;
import com.fairing.fairplay.reservation.entity.QReservation;
import com.fairing.fairplay.payment.entity.QPayment;
import com.fairing.fairplay.statistics.entity.hourly.QEventHourlyStatistics;
import com.fairing.fairplay.statistics.entity.sales.EventDailySalesStatistics;
import com.fairing.fairplay.statistics.entity.sales.QEventDailySalesStatistics;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class HourlyStatsCustomRepositoryImpl implements HourlyStatsCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<EventHourlyStatistics> calculateWithRevenueByEventAndDateRange(Long eventId, LocalDate startDate, LocalDate endDate) {
        // Payment 테이블에서 직접 시간별 예매 건수와 매출 조회
        QReservation r = QReservation.reservation;
        QPayment p = QPayment.payment;

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        List<Tuple> results = queryFactory
                .select(
                        r.event.eventId,
                        r.createdAt,
                        r.createdAt.hour(),
                        r.createdAt.dayOfMonth(),
                        r.count(),
                        p.amount.sum().coalesce(BigDecimal.ZERO)
                )
                .from(r)
                .leftJoin(p).on(p.reservation.eq(r))
                .where(
                        r.event.eventId.eq(eventId)
                                .and(r.createdAt.between(start, end))
                                .and(p.paidAt.between(start, end)) // 결제가 같은 기간에 이루어진 경우만
                )
                .groupBy(r.event.eventId, r.createdAt.hour(), r.createdAt.dayOfMonth())
                .fetch();

        return results.stream()
                .map(t -> EventHourlyStatistics.builder()
                        .eventId(t.get(r.event.eventId))
                        .statDate(t.get(r.createdAt).toLocalDate())
                        .hour(t.get(r.createdAt.hour()))
                        .reservations(t.get(r.count()).longValue())
                        .totalRevenue(t.get(p.amount.sum()))
                        .createdAt(LocalDateTime.now())
                        .build())
                .toList();
    }

    // 매출 계산 헬퍼 메서드
    private BigDecimal calculateHourlyRevenue(Long eventId, LocalDate date, Integer hour, Long hourReservations) {
        QPayment p = QPayment.payment;
        QReservation r = QReservation.reservation;

        LocalDateTime hourStart = date.atTime(hour, 0);
        LocalDateTime hourEnd = date.atTime(hour + 1, 0);

        // 1. 실제 결제 시각 기반 합산 (정확한 방식)
        BigDecimal realRevenue = queryFactory
                .select(p.amount.sum().coalesce(BigDecimal.valueOf(0)))
                .from(p)
                .join(p.reservation, r)
                .where(
                        r.event.eventId.eq(eventId),
                        p.paidAt.between(hourStart, hourEnd),
                        p.paymentStatusCode.code.eq("COMPLETED")
                )
                .fetchOne();

        if (realRevenue != null && realRevenue.compareTo(BigDecimal.ZERO) > 0) {
            return realRevenue; // 결제 시각으로 계산된 매출 있으면 바로 반환
        }

        // 2. Fallback - 기존 비례 배분 방식
        QEventDailySalesStatistics dailySales = QEventDailySalesStatistics.eventDailySalesStatistics;

        EventDailySalesStatistics dailyStat = queryFactory
                .selectFrom(dailySales)
                .where(
                        dailySales.eventId.eq(eventId),
                        dailySales.statDate.eq(date)
                )
                .fetchOne();

        if (dailyStat == null || dailyStat.getTotalSales() == 0L) {
            return BigDecimal.ZERO;
        }

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        Long totalDailyReservations = queryFactory
                .select(r.count())
                .from(r)
                .where(
                        r.event.eventId.eq(eventId),
                        r.createdAt.between(dayStart, dayEnd)
                )
                .fetchOne();

        if (totalDailyReservations == null || totalDailyReservations == 0L) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalSales = BigDecimal.valueOf(dailyStat.getTotalSales());
        BigDecimal hourlyRatio = BigDecimal.valueOf(hourReservations)
                .divide(BigDecimal.valueOf(totalDailyReservations), 4, RoundingMode.HALF_UP);

        return totalSales.multiply(hourlyRatio);
    }

    @Override
    public List<EventHourlyStatistics> calculate(LocalDate targetDate) {
        QReservation r = QReservation.reservation;
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

        List<Tuple> results = queryFactory
                .select(r.event.eventId, r.createdAt.hour(), r.count())
                .from(r)
                .where(r.createdAt.between(start, end))
                .groupBy(r.event.eventId, r.createdAt.hour())
                .fetch();

        return results.stream()
                .map(t -> EventHourlyStatistics.builder()
                        .eventId(t.get(r.event.eventId))
                        .statDate(targetDate)
                        .hour(t.get(r.createdAt.hour()))
                        .reservations(t.get(r.count()).longValue())
                        .createdAt(LocalDateTime.now())
                        .build())
                .toList();
    }

    @Override
    public List<EventHourlyStatistics> calculateWithRevenue(LocalDate targetDate) {
        // Payment 테이블에서 직접 시간별 예매 건수와 매출 조회
        QReservation r = QReservation.reservation;
        QPayment p = QPayment.payment;

        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

        List<Tuple> results = queryFactory
                .select(
                        r.event.eventId,
                        r.createdAt.hour(),
                        r.count(),
                        p.amount.sum().coalesce(BigDecimal.ZERO)
                )
                .from(r)
                .leftJoin(p).on(p.reservation.eq(r))
                .where(
                        r.createdAt.between(start, end)
                                .and(p.paidAt.isNull().or(p.paidAt.between(start, end))) // 결제가 같은 날에 이루어진 경우만
                )
                .groupBy(r.event.eventId, r.createdAt.hour())
                .fetch();

        return results.stream()
                .map(t -> EventHourlyStatistics.builder()
                        .eventId(t.get(r.event.eventId))
                        .statDate(targetDate)
                        .hour(t.get(r.createdAt.hour()))
                        .reservations(t.get(r.count()).longValue())
                        .totalRevenue(t.get(p.amount.sum()))
                        .createdAt(LocalDateTime.now())
                        .build())
                .toList();
    }

    @Override
    public List<EventHourlyStatistics> findByEventIdAndDateRange(Long eventId, LocalDate startDate, LocalDate endDate) {
        QEventHourlyStatistics ehs = QEventHourlyStatistics.eventHourlyStatistics;

        return queryFactory
                .selectFrom(ehs)
                .where(
                        ehs.eventId.eq(eventId)
                                .and(ehs.statDate.between(startDate, endDate))
                )
                .orderBy(ehs.statDate.asc(), ehs.hour.asc())
                .fetch();
    }

    @Override
    public Optional<EventHourlyStatistics> findPeakHourForDate(LocalDate targetDate) {
        QEventHourlyStatistics ehs = QEventHourlyStatistics.eventHourlyStatistics;

        EventHourlyStatistics result = queryFactory
                .selectFrom(ehs)
                .where(ehs.statDate.eq(targetDate))
                .orderBy(ehs.reservations.desc())
                .limit(1)
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public List<EventHourlyStatistics> findTop5PeakHours(LocalDate targetDate) {
        QEventHourlyStatistics ehs = QEventHourlyStatistics.eventHourlyStatistics;

        return queryFactory
                .selectFrom(ehs)
                .where(ehs.statDate.eq(targetDate))
                .orderBy(ehs.reservations.desc())
                .limit(5)
                .fetch();
    }
}