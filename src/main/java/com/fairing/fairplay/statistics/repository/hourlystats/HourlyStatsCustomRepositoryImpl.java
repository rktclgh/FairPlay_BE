package com.fairing.fairplay.statistics.repository.hourlystats;

import com.fairing.fairplay.statistics.entity.hourly.EventHourlyStatistics;
import com.fairing.fairplay.reservation.entity.QReservation;
import com.fairing.fairplay.payment.entity.QPayment;
import com.fairing.fairplay.payment.entity.QPaymentTargetType;
import com.fairing.fairplay.statistics.entity.hourly.QEventHourlyStatistics;
import com.fairing.fairplay.statistics.entity.sales.EventDailySalesStatistics;
import com.fairing.fairplay.statistics.entity.sales.QEventDailySalesStatistics;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
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

        QPaymentTargetType ptt = QPaymentTargetType.paymentTargetType;


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
                .join(p).on(
                p.targetId.eq(r.reservationId)
                        .and(p.paymentTargetType.paymentTargetCode.eq("RESERVATION"))
                )
                .leftJoin(p.paymentTargetType, ptt)
                .where(
                        r.event.eventId.eq(eventId)
                                .and(r.createdAt.between(start, end))
                                .and(p.paidAt.isNotNull())
                                .and(p.paidAt.between(start, end))
                                .and(p.paymentStatusCode.paymentStatusCodeId.eq(2))

                )
                .groupBy(r.event.eventId, r.createdAt.hour(), r.createdAt.dayOfMonth())
                .fetch();



        return results.stream()
                .map(t -> {
                    BigDecimal revenue = t.get(p.amount.sum().coalesce(BigDecimal.ZERO)); // 이미 가져온 값

                    return EventHourlyStatistics.builder()
                            .eventId(t.get(r.event.eventId))
                            .statDate(t.get(r.createdAt).toLocalDate())
                            .hour(t.get(r.createdAt.hour()))
                            .reservations(t.get(r.count()).longValue())
                            .totalRevenue(revenue) // 여기 변수 사용
                            .createdAt(LocalDateTime.now())
                            .build();
                })
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
                .join(r).on(p.targetId.eq(r.reservationId)
                        .and(p.paymentTargetType.paymentTargetCode.eq("RESERVATION")))
                .where(
                        r.event.eventId.eq(eventId),
                        p.paidAt.between(hourStart, hourEnd),
                        p.paymentStatusCode.paymentStatusCodeId.eq(2) // COMPLETED 상태 (하드코딩)
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
                        r.reservationId.countDistinct(),                     // 중복 제거한 예약 수
                        p.amount.sum().coalesce(BigDecimal.ZERO)
                                )
                .from(r)
                .innerJoin(p).on(                                        // 결제 완료된 예약만 조인
                        p.targetId.eq(r.reservationId)
                        .and(p.paymentTargetType.paymentTargetCode.eq("RESERVATION"))
                .and(p.paidAt.isNotNull())
                .and(p.paidAt.between(start, end))
                .and(p.paymentStatusCode.paymentStatusCodeId.eq(2)))
                .where(r.createdAt.between(start, end))
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

    @Override
    public List<EventHourlyStatistics> calculateByDayOfWeek(Long eventId, LocalDate startDate, LocalDate endDate) {
        QReservation r = QReservation.reservation;
        QPayment p = QPayment.payment;

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        List<Tuple> results = queryFactory
                .select(
                        r.event.eventId,
                        r.createdAt.dayOfWeek(),
                        r.createdAt.hour(),
                        r.count(),
                        p.amount.sum().coalesce(BigDecimal.ZERO)
                )
                .from(r)
                .leftJoin(p).on(
                        p.targetId.eq(r.reservationId)
                                .and(p.paymentTargetType.paymentTargetCode.eq("RESERVATION"))
                                .and(p.paidAt.isNotNull())
                                .and(p.paidAt.between(start, end))
                                .and(p.paymentStatusCode.paymentStatusCodeId.eq(2))
                )
                .where(
                        r.event.eventId.eq(eventId)
                                .and(r.createdAt.between(start, end))
                )
                .groupBy(r.event.eventId, r.createdAt.dayOfWeek(), r.createdAt.hour())
                .orderBy(r.createdAt.dayOfWeek().asc(), r.createdAt.hour().asc())
                .fetch();

        return results.stream()
                .map(t -> EventHourlyStatistics.builder()
                        .eventId(t.get(r.event.eventId))
                        .statDate(startDate)
                        .hour(t.get(r.createdAt.hour()))
                        .reservations(t.get(r.count()).longValue())
                        .totalRevenue(t.get(p.amount.sum()))
                        .createdAt(LocalDateTime.now())
                        .build())
                .toList();
    }

    @Override
    public List<EventHourlyStatistics> calculateDailySummaryByDayOfWeek(Long eventId, LocalDate startDate, LocalDate endDate) {
        QReservation r = QReservation.reservation;
        QPayment p = QPayment.payment;

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        List<Tuple> results = queryFactory
                .select(
                        r.event.eventId,
                        // DB별 요일 반환값을 월=1..일=7로 표준화
                        new CaseBuilder()
                        .when(r.createdAt.dayOfWeek().eq(1)).then(7)  // SQL Sunday=1 → 7
                                        .otherwise(r.createdAt.dayOfWeek().subtract(1)).as("dayOfWeek"),
                        // 중복 예약 제거
                        r.reservationId.countDistinct(),
                        p.amount.sum().coalesce(BigDecimal.ZERO)
                )
                .from(r)
                .leftJoin(p).on(
                        p.targetId.eq(r.reservationId)
                                .and(p.paymentTargetType.paymentTargetCode.eq("RESERVATION"))
                                .and(p.paidAt.isNotNull())
                                .and(p.paidAt.between(start, end))
                                .and(p.paymentStatusCode.paymentStatusCodeId.eq(2))
                )
                .where(
                        r.event.eventId.eq(eventId)
                                .and(r.createdAt.between(start, end))
                )
                .groupBy(r.event.eventId, r.createdAt.dayOfWeek())
                .orderBy(r.createdAt.dayOfWeek().asc())
                .fetch();

        return results.stream()
                .map(t -> {
                    Integer dayOfWeek = t.get(r.createdAt.dayOfWeek());
                    BigDecimal revenue = t.get(p.amount.sum().coalesce(BigDecimal.ZERO));
                    return EventHourlyStatistics.builder()
                            .eventId(t.get(r.event.eventId))
                            .statDate(startDate)
                            .hour(dayOfWeek)
                            .reservations(t.get(r.count()).longValue())
                            .totalRevenue(revenue)
                            .createdAt(LocalDateTime.now())
                            .build();
                })
                .toList();
    }

    @Override
    public List<EventHourlyStatistics> calculateMonthlyTimePeriodSummary(Long eventId, LocalDate startDate, LocalDate endDate) {
        QReservation r = QReservation.reservation;
        QPayment p = QPayment.payment;

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        List<Tuple> results = queryFactory
                .select(
                        r.event.eventId,
                        r.createdAt.month(),
                        new CaseBuilder()
                                .when(r.createdAt.hour().between(6, 11)).then(1)
                                .when(r.createdAt.hour().between(12, 17)).then(2)
                                .when(r.createdAt.hour().between(18, 23)).then(3)
                                .otherwise(0).as("timePeriod"),
                        r.count()
                )
                .from(r)
                .leftJoin(p).on(
                        p.targetId.eq(r.reservationId)
                                .and(p.paymentTargetType.paymentTargetCode.eq("RESERVATION"))
                                .and(p.paidAt.isNotNull())
                                .and(p.paidAt.between(start, end))
                                .and(p.paymentStatusCode.paymentStatusCodeId.eq(2))
                )
                .where(
                        r.event.eventId.eq(eventId)
                                .and(r.createdAt.between(start, end))
                )
                .groupBy(r.event.eventId, r.createdAt.month(), 
                        new CaseBuilder()
                                .when(r.createdAt.hour().between(6, 11)).then(1)
                                .when(r.createdAt.hour().between(12, 17)).then(2)
                                .when(r.createdAt.hour().between(18, 23)).then(3)
                                .otherwise(0))
                .orderBy(r.createdAt.month().asc())
                .fetch();

        return results.stream()
                .map(t -> {
                    Integer month = t.get(r.createdAt.month());
                    Integer timePeriod = t.get(Expressions.numberPath(Integer.class, "timePeriod"));
                    return EventHourlyStatistics.builder()
                            .eventId(t.get(r.event.eventId))
                            .statDate(startDate)
                            .hour(month * 10 + timePeriod)
                            .reservations(t.get(r.count()).longValue())
                            .totalRevenue(BigDecimal.ZERO)
                            .createdAt(LocalDateTime.now())
                            .build();
                })
                .toList();
    }
}