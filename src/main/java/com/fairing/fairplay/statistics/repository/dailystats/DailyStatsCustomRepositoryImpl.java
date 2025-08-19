package com.fairing.fairplay.statistics.repository.dailystats;

import com.fairing.fairplay.event.entity.QEventDetail;
import com.fairing.fairplay.qr.entity.QQrTicket;
import com.fairing.fairplay.statistics.dto.event.EventPopularityStatisticsListDto;
import com.fairing.fairplay.statistics.dto.reservation.AdminReservationStatsByCategoryDto;
import com.fairing.fairplay.statistics.dto.reservation.AdminReservationStatsListDto;
import com.fairing.fairplay.statistics.entity.event.QEventPopularityStatistics;
import com.fairing.fairplay.statistics.entity.reservation.EventDailyStatistics;
import com.fairing.fairplay.reservation.entity.QReservation;
import com.fairing.fairplay.reservation.entity.QReservationStatusCode;
import com.fairing.fairplay.attendee.entity.QAttendee;
import com.fairing.fairplay.qr.entity.QQrCheckLog;
import com.fairing.fairplay.statistics.entity.reservation.QEventDailyStatistics;
import com.fairing.fairplay.statistics.entity.sales.QEventDailySalesStatistics;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class DailyStatsCustomRepositoryImpl implements DailyStatsCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<EventDailyStatistics> calculate(LocalDate targetDate) {
        QReservation r = QReservation.reservation;
        QReservationStatusCode statusCode = QReservationStatusCode.reservationStatusCode;
        QAttendee a = QAttendee.attendee;
        QQrCheckLog q = QQrCheckLog.qrCheckLog;
        QQrTicket qrt = QQrTicket.qrTicket;

        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

        // 1. 예약 / 취소 수
        List<Tuple> reservationResults = queryFactory
                .select(r.event.eventId, statusCode.code, r.count())
                .from(r)
                .join(statusCode)
                .on(r.reservationStatusCode.id.eq(statusCode.reservationStatusCode.id))
                .where(r.createdAt.goe(start).and(r.createdAt.lt(end)))
                .groupBy(r.event.eventId, statusCode.code)
                .fetch();

        // 2. 체크인 수
        List<Tuple> checkinResults = queryFactory
                .select(r.event.eventId, a.id.countDistinct())
                .from(a)
                .join(r).on(a.reservation.eq(r))
                .join(qrt).on(qrt.attendee.eq(a))           // QrTicket 조인 추가
                .join(q).on(q.qrTicket.eq(qrt))
                .where(a.checkedIn.isTrue()
                        .and(q.createdAt.goe(start).and(q.createdAt.lt(end))))
                .groupBy(r.event.eventId)
                .fetch();

        Map<Long, EventDailyStatistics> map = new HashMap<>();

        // 예약 & 취소 반영
        for (Tuple t : reservationResults) {
            Long eventId = t.get(r.event.eventId);
            String status = t.get(statusCode.code);
            Long count = t.get(r.count());

            EventDailyStatistics stat = map.computeIfAbsent(eventId, id ->
                    EventDailyStatistics.builder()
                            .eventId(eventId)
                            .statDate(targetDate)
                            .reservationCount(0)
                            .checkinsCount(0)
                            .cancellationCount(0)
                            .noShowsCount(0)
                            .createdAt(LocalDateTime.now())
                            .build()
            );

            switch (status) {
                case "CONFIRMED" -> stat.setReservationCount(stat.getReservationCount() + count.intValue());
                case "CANCELLED", "REFUNDED" -> stat.setCancellationCount(stat.getCancellationCount() + count.intValue());
            }
        }

        // 체크인 수 반영
        for (Tuple t : checkinResults) {
            Long eventId = t.get(r.event.eventId);
            Long checkins = t.get(a.id.countDistinct());

            EventDailyStatistics stat = map.computeIfAbsent(eventId, id ->
                    EventDailyStatistics.builder()
                            .eventId(eventId)
                            .statDate(targetDate)
                            .reservationCount(0)
                            .checkinsCount(0)
                            .cancellationCount(0)
                            .noShowsCount(0)
                            .createdAt(LocalDateTime.now())
                            .build()
            );

            stat.setCheckinsCount(checkins.intValue());
        }

        // 노쇼 수 = 예약 확정 - 체크인
        map.values().forEach(stat -> {
            int noShows = stat.getReservationCount() - stat.getCheckinsCount();
            stat.setNoShowsCount(Math.max(noShows, 0));
        });

        return new ArrayList<>(map.values());
    }

    @Override
    public List<AdminReservationStatsByCategoryDto> aggregatedReservationByCategory(
            LocalDate startDate,
            LocalDate endDate


    ) {
        QEventDailyStatistics eds = QEventDailyStatistics.eventDailyStatistics;
        QEventDetail d = QEventDetail.eventDetail;

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(
                eds.statDate.goe(LocalDate.from(startDate.atStartOfDay()))
                        .and(eds.statDate.lt(LocalDate.from(endDate.plusDays(1).atStartOfDay())))
        );



        return queryFactory
                .select(Projections.constructor(AdminReservationStatsByCategoryDto.class,
                        eds.reservationCount.sum().coalesce(0),
                        d.mainCategory

                ))
                .from(eds)
                .leftJoin(d).on(d.event.eventId.eq(eds.eventId))
                .where(builder)
                .groupBy(d.mainCategory)
                .fetch();
    }

    /**
     * 검색
     * */
    @Override
    public Page<AdminReservationStatsListDto> searchEventReservationWithRank(LocalDate startDate, LocalDate endDate, String keyword, String mainCategory, String subCategory, Pageable pageable) {
        QEventPopularityStatistics eps = QEventPopularityStatistics.eventPopularityStatistics;
        QEventDetail d = QEventDetail.eventDetail;
        QEventDailySalesStatistics edss = QEventDailySalesStatistics.eventDailySalesStatistics;

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(
                eps.calculatedAt.goe(startDate.atStartOfDay())
                        .and(eps.calculatedAt.lt(endDate.plusDays(1).atStartOfDay()))
        );

        if (keyword != null && !keyword.isBlank()) {
            builder.and(eps.eventTitle.containsIgnoreCase(keyword));
        }
        if (mainCategory != null && !mainCategory.isBlank()) {
            builder.and(d.mainCategory.groupName.stringValue().eq(mainCategory));
        }
        if (subCategory != null && !subCategory.isBlank()) {
            builder.and(d.subCategory.categoryName.stringValue().eq(subCategory));
        }

        List<AdminReservationStatsListDto> content = queryFactory
                .select(Projections.constructor(AdminReservationStatsListDto.class,
                        eps.eventId,
                        eps.eventTitle,
                        eps.reservationCount.sum().coalesce(0L),
                        edss.totalSales.sum().coalesce(0L),
                        d.mainCategory,
                        d.subCategory,
                        Expressions.numberTemplate(Integer.class,
                                "ROW_NUMBER() OVER (ORDER BY {0} DESC)", eps.viewCount.sum().coalesce(0L)),
                        eps.calculatedAt.max()
                ))
                .from(eps)
                .leftJoin(d).on(d.event.eventId.eq(eps.eventId))
                .leftJoin(edss).on(edss.eventId.eq(eps.eventId)
                        .and(edss.statDate.goe(startDate))
                        .and(edss.statDate.lt(endDate.plusDays(1))))
                .where(builder)
                .groupBy(
                        eps.eventId,
                        eps.eventTitle,
                        d.mainCategory,
                        d.subCategory
                )
                .orderBy(eps.reservationCount.sum().desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(eps.eventId.countDistinct())
                .from(eps)
                .leftJoin(d).on(d.event.eventId.eq(eps.eventId))
                .leftJoin(edss).on(edss.eventId.eq(eps.eventId))
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);

    }

    /***
     * 행사별 예약 순위
     *
     */
    @Override
    public Page<AdminReservationStatsListDto> aggregatedEvents(
            LocalDate startDate,
            LocalDate endDate,
            String mainCategory,
            String subCategory,
            Pageable pageable  // 페이징 정보 추가
    ) {
        QEventPopularityStatistics eps = QEventPopularityStatistics.eventPopularityStatistics;
        QEventDetail d = QEventDetail.eventDetail;
        QEventDailySalesStatistics edss = QEventDailySalesStatistics.eventDailySalesStatistics;

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(
                eps.calculatedAt.goe(startDate.atStartOfDay())
                        .and(eps.calculatedAt.lt(endDate.plusDays(1).atStartOfDay()))
        );

        if (mainCategory != null && !mainCategory.isBlank()) {
            builder.and(d.mainCategory.groupName.stringValue().eq(mainCategory));
        }

        if (subCategory != null && !subCategory.isBlank()) {
            builder.and(d.subCategory.categoryName.stringValue().eq(subCategory));
        }

        // 데이터 조회 쿼리
        List<AdminReservationStatsListDto> content = queryFactory
                .select(Projections.constructor(AdminReservationStatsListDto.class,
                        eps.eventId,
                        eps.eventTitle,
                        eps.reservationCount.sum().coalesce(0L),
                        edss.totalSales.sum().coalesce(0L),
                        d.mainCategory,
                        d.subCategory,
                        Expressions.numberTemplate(Integer.class,
                                "ROW_NUMBER() OVER (ORDER BY {0} DESC)", eps.reservationCount.sum().coalesce(0L)),
                        eps.calculatedAt.max()
                ))
                .from(eps)
                .leftJoin(d).on(d.event.eventId.eq(eps.eventId))
                .leftJoin(edss).on(edss.eventId.eq(eps.eventId)
                        .and(edss.statDate.goe(startDate))
                        .and(edss.statDate.lt(endDate.plusDays(1))))
                .where(builder)
                .groupBy(eps.eventId, eps.eventTitle,d.mainCategory, d.subCategory)
                .orderBy(eps.reservationCount.sum().desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 카운트 쿼리 (group by가 있으므로 countDistinct 사용 권장)
        Long total = queryFactory
                .select(eps.eventId.countDistinct())
                .from(eps)
                .leftJoin(d).on(d.event.eventId.eq(eps.eventId))
                .leftJoin(edss).on(edss.eventId.eq(eps.eventId))
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

}
