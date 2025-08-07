package com.fairing.fairplay.statistics.repository.ticketstats;

import com.fairing.fairplay.statistics.entity.reservation.EventTicketStatistics;
import com.fairing.fairplay.reservation.entity.QReservation;
import com.fairing.fairplay.ticket.entity.QTicket;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TicketStatsCustomRepositoryImpl implements TicketStatsCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<EventTicketStatistics> calculate(LocalDate targetDate) {
        QReservation r = QReservation.reservation;
        QTicket t = QTicket.ticket;

        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

        List<Tuple> results = queryFactory
                .select(r.event.eventId, t.name, r.count())
                .from(r)
                .join(t).on(r.ticket.ticketId.eq(t.ticketId))
                .where(r.createdAt.between(start, end))
                .groupBy(r.event.eventId, t.name)
                .fetch();

        return results.stream()
                .map(tuple -> EventTicketStatistics.builder()
                        .eventId(tuple.get(r.event.eventId))
                        .statDate(targetDate)
                        .ticketType(tuple.get(t.name))
                        .reservations(tuple.get(r.count()).intValue())
                        .createdAt(LocalDateTime.now())
                        .build())
                .toList();
    }
}
