package com.fairing.fairplay.statistics.repository.eventstats;

import com.fairing.fairplay.statistics.entity.event.EventComparisonStatistics;
import com.fairing.fairplay.reservation.entity.QReservation;
import com.fairing.fairplay.payment.entity.QPayment;
import com.fairing.fairplay.event.entity.QEvent;
import com.fairing.fairplay.event.entity.QEventDetail;
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

@Repository
@RequiredArgsConstructor
public class EventComparisonStatsCustomRepositoryImpl implements EventComparisonStatsCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<EventComparisonStatistics> calculate(LocalDate targetDate) {
        QEvent e = QEvent.event;
        QReservation r = QReservation.reservation;
        QPayment p = QPayment.payment;
        QEventDailySalesStatistics dailySales = QEventDailySalesStatistics.eventDailySalesStatistics;
        QEventDetail d = QEventDetail.eventDetail;

        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

        // 이벤트별 기본 통계 조회
        List<Tuple> results = queryFactory
                .select(
                        e.eventId,
                        e.titleKr, // 이벤트명
                        r.countDistinct().coalesce(0L), // 총 예약자 수 (중복 제거)
                        r.count().coalesce(0L), // 총 예약 건수
                        dailySales.paidSales.sum().coalesce(0L), // 총 매출 (일별 매출 합계)
                        dailySales.paidCount.sum().coalesce(0), // 총 결제 건수
                        dailySales.totalCount.sum().coalesce(0),
                        dailySales.cancelledCount.sum().coalesce(0),
                        d.startDate,
                        d.endDate
                )
                .from(e)
                .leftJoin(r).on(r.event.eq(e))
                .leftJoin(dailySales).on(
                        dailySales.eventId.eq(e.eventId)
                                .and(dailySales.statDate.eq(targetDate))
                )
                .leftJoin(d).on(d.event.eq(e))
                .where(d.createdAt.loe(end)
                .and(r.createdAt.between(start, end))
                .and(dailySales.statDate.eq(targetDate)))
                .groupBy(e.eventId, e.titleKr, d.startDate, d.endDate)
                .fetch();

        return results.stream()
                .map(t -> {
                    Long eventId = t.get(e.eventId);
                    String eventTitle = t.get(e.titleKr);
                    Long totalUsers = t.get(r.countDistinct());
                    Long totalReservations = t.get(r.count());
                    Long totalSales = t.get(dailySales.paidSales.sum());
                    Integer paidCount = t.get(dailySales.paidCount.sum());
                    BigDecimal cancellationRate = t.get(dailySales.totalCount) > 0
                            ? BigDecimal.valueOf(t.get(dailySales.cancelledCount) * 100.0 / t.get(dailySales.totalCount)).setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    // 평균 티켓 가격 계산
                    Long avgTicketPrice = (paidCount != null && paidCount > 0)
                            ? totalSales / paidCount
                            : 0L;

                    return EventComparisonStatistics.builder()
                            .eventId(eventId)
                            .eventTitle(eventTitle)
                            .totalUsers(totalUsers)
                            .totalReservations(totalReservations)
                            .totalSales(totalSales)
                            .avgTicketPrice(avgTicketPrice)
                            .cancellationRate(cancellationRate)
                            .startDate(t.get(d.startDate))
                            .endDate(t.get(d.endDate))
                            .lastUpdatedAt(LocalDateTime.now())
                            .build();
                })
                .toList();
    }



    // 이벤트 상태별 조회 메서드 추가
    public List<EventComparisonStatistics> findByStatus(String status, LocalDate currentDate) {
        QEvent e = QEvent.event;
        QReservation r = QReservation.reservation;
        QEventDailySalesStatistics dailySales = QEventDailySalesStatistics.eventDailySalesStatistics;
        QEventDetail d = QEventDetail.eventDetail;

        var query = queryFactory
                .select(
                        e.eventId,
                        e.titleKr,
                        r.countDistinct().coalesce(0L),
                        r.count().coalesce(0L),
                        dailySales.paidSales.sum().coalesce(0L),
                        dailySales.paidCount.sum().coalesce(0),
                        dailySales.totalCount.sum().coalesce(0),
                        dailySales.cancelledCount.sum().coalesce(0),
                        d.startDate,
                        d.endDate
                )
                .from(e)
                .leftJoin(r).on(r.event.eq(e))
                .leftJoin(dailySales).on(dailySales.eventId.eq(e.eventId))
                .leftJoin(d).on(d.event.eq(e));

        // 상태별 필터링
        String normalized = (status == null) ? "all" : status.toLowerCase();
        switch (normalized) {
            case "ongoing":
                query.where(d.startDate.loe(currentDate).and(d.endDate.goe(currentDate)));
                break;
            case "ended":
                query.where(d.endDate.lt(currentDate));
                break;
            case "upcoming":
                query.where(d.startDate.gt(currentDate));
                break;
            default:
                // "all" - 모든 이벤트
                break;
        }

        List<Tuple> results = query
                .groupBy(e.eventId, e.titleKr, d.startDate, d.endDate)
                .fetch();

        return results.stream()
                .map(t -> {
                    Long paidSales = t.get(dailySales.paidSales.sum());
                    Integer paidCount = t.get(dailySales.paidCount.sum());
                    Integer totalCount = t.get(dailySales.totalCount.sum());
                    Integer cancelledCount = t.get(dailySales.cancelledCount.sum());

                    // 평균 티켓 가격
                    Long avgTicketPrice = (paidCount != null && paidCount > 0)
                            ? paidSales / paidCount
                            : 0L;

                    // 취소율 계산
                    BigDecimal cancellationRate = (totalCount != null && totalCount > 0)
                            ? BigDecimal.valueOf((cancelledCount * 100.0) / totalCount)
                            .setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    return EventComparisonStatistics.builder()
                            .eventId(t.get(e.eventId))
                            .eventTitle(t.get(e.titleKr))
                            .totalUsers(t.get(r.countDistinct()))
                            .totalReservations(t.get(r.count()))
                            .totalSales(paidSales)
                            .avgTicketPrice(avgTicketPrice)
                            .cancellationRate(cancellationRate)
                            .startDate(t.get(d.startDate))
                            .endDate(t.get(d.endDate))
                            .lastUpdatedAt(LocalDateTime.now())
                            .build();
                })
                .toList();
    }

    /*private Long calculateAvgTicketPrice(Long totalSales, Long totalCount) {
        if (totalCount == null || totalCount == 0) return 0L;
        return totalSales / totalCount;

    }*/


    /*private BigDecimal calculateCancellationRate(Long eventId) {
        QEventDailySalesStatistics dailySales = QEventDailySalesStatistics.eventDailySalesStatistics;

        // 일별 매출 통계에서 총 건수와 취소 건수 조회
        Tuple result = queryFactory
                .select(
                        dailySales.totalCount.sum().coalesce(0),
                        dailySales.cancelledCount.sum().coalesce(0)
                )
                .from(dailySales)
                .where(dailySales.eventId.eq(eventId))
                .fetchOne();

        if (result == null) return BigDecimal.ZERO;

        Integer totalCount = result.get(dailySales.totalCount.sum());
        Integer cancelledCount = result.get(dailySales.cancelledCount.sum());

        if (totalCount == null || totalCount == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(cancelledCount != null ? cancelledCount : 0)
                .divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }*/
}