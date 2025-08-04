package com.fairing.fairplay.statistics.repository.dailystats;

import com.fairing.fairplay.statistics.entity.EventDailyStatistics;
import com.fairing.fairplay.reservation.entity.QReservation;
import com.fairing.fairplay.reservation.entity.QReservationStatusCode;
import com.fairing.fairplay.attendee.entity.QAttendee;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
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

        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

        // 1. 예약 / 취소 수
        List<Tuple> reservationResults = queryFactory
                .select(r.event.eventId, statusCode.code, r.count())
                .from(r)
                .join(statusCode)
                .on(r.reservationStatusCode.id.eq(statusCode.reservationStatusCode.id))
                .where(r.createdAt.between(start, end))
                .groupBy(r.event.eventId, statusCode.code)
                .fetch();

        // 2. 체크인 수
        List<Tuple> checkinResults = queryFactory
                .select(r.event.eventId, a.count())
                .from(a)
                .join(r).on(a.reservation.eq(r))
                .where(a.checkedIn.isTrue()
                        .and(r.createdAt.between(start, end)))
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
            Long checkins = t.get(a.count());

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
}
