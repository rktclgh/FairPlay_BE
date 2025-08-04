package com.fairing.fairplay.statistics.repository.hourlystats;

import com.fairing.fairplay.statistics.entity.EventHourlyStatistics;
import com.fairing.fairplay.reservation.entity.QReservation;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class HourlyStatsCustomRepositoryImpl implements HourlyStatsCustomRepository {

    private final JPAQueryFactory queryFactory;

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
}