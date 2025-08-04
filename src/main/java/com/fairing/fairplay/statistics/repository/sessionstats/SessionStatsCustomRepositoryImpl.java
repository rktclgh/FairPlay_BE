package com.fairing.fairplay.statistics.repository.sessionstats;

import com.fairing.fairplay.statistics.entity.EventSessionStatistics;
import com.fairing.fairplay.reservation.entity.QReservation;
import com.fairing.fairplay.reservation.entity.QReservationStatusCode;
import com.fairing.fairplay.attendee.entity.QAttendee;
import com.fairing.fairplay.ticket.entity.QTicket;
import com.fairing.fairplay.ticket.entity.QEventSchedule;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class SessionStatsCustomRepositoryImpl implements SessionStatsCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<EventSessionStatistics> calculate(LocalDate targetDate) {
        QReservation r = QReservation.reservation;
        QReservationStatusCode statusCode = QReservationStatusCode.reservationStatusCode;
        QAttendee a = QAttendee.attendee;
        QTicket t = QTicket.ticket;
        QEventSchedule s = QEventSchedule.eventSchedule;

        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

        // 1. 예약/취소 집계
        List<Tuple> reservationResults = queryFactory
                .select(r.event.eventId, r.schedule.scheduleId, t.name, statusCode.code, r.count())
                .from(r)
                .join(statusCode).on(r.reservationStatusCode.id.eq(statusCode.reservationStatusCode.id))
                .join(t).on(r.ticket.ticketId.eq(t.ticketId))
                .join(s).on(r.schedule.scheduleId.eq(s.scheduleId))
                .where(r.createdAt.between(start, end))
                .groupBy(r.event.eventId, r.schedule.scheduleId, t.name, statusCode.code)
                .fetch();

        // 2. 체크인 집계
        List<Tuple> checkinResults = queryFactory
                .select(r.event.eventId, r.schedule.scheduleId, t.name, a.count())
                .from(a)
                .join(r).on(a.reservation.eq(r))
                .join(t).on(r.ticket.ticketId.eq(t.ticketId))
                .join(s).on(r.schedule.scheduleId.eq(s.scheduleId))
                .where(a.checkedIn.isTrue()
                        .and(r.createdAt.between(start, end)))
                .groupBy(r.event.eventId, r.schedule.scheduleId, t.name)
                .fetch();

        // Map<eventId, Map<sessionId, Map<ticketName, EventSessionStatistics>>>
        Map<Long, Map<Long, Map<String, EventSessionStatistics>>> statsMap = new HashMap<>();

        // 예약/취소 수 반영
        for (Tuple row : reservationResults) {
            Long eventId = row.get(r.event.eventId);
            Long sessionId = row.get(r.schedule.scheduleId);
            String ticketName = row.get(t.name);
            String status = row.get(statusCode.code);
            Long count = row.get(r.count());

            EventSessionStatistics stat = statsMap
                    .computeIfAbsent(eventId, e -> new HashMap<>())
                    .computeIfAbsent(sessionId, sId -> new HashMap<>())
                    .computeIfAbsent(ticketName, tn ->
                            EventSessionStatistics.builder()
                                    .eventId(eventId)
                                    .sessionId(sessionId)
                                    .ticketType(ticketName)
                                    .statDate(targetDate)
                                    .reservations(0)
                                    .checkins(0)
                                    .cancellations(0)
                                    .noShows(0)
                                    .createdAt(LocalDateTime.now())
                                    .build()
                    );

            switch (status) {
                case "CONFIRMED" -> stat.setReservations(stat.getReservations() + count.intValue());
                case "CANCELLED", "REFUNDED" -> stat.setCancellations(stat.getCancellations() + count.intValue());
            }
        }

        // 체크인 수 반영
        for (Tuple row : checkinResults) {
            Long eventId = row.get(r.event.eventId);
            Long sessionId = row.get(r.schedule.scheduleId);
            String ticketName = row.get(t.name);
            Long checkinCount = row.get(a.count());

            EventSessionStatistics stat = statsMap
                    .computeIfAbsent(eventId, e -> new HashMap<>())
                    .computeIfAbsent(sessionId, sId -> new HashMap<>())
                    .computeIfAbsent(ticketName, tn ->
                            EventSessionStatistics.builder()
                                    .eventId(eventId)
                                    .sessionId(sessionId)
                                    .ticketType(ticketName)
                                    .statDate(targetDate)
                                    .reservations(0)
                                    .checkins(0)
                                    .cancellations(0)
                                    .noShows(0)
                                    .createdAt(LocalDateTime.now())
                                    .build()
                    );

            stat.setCheckins(checkinCount.intValue());
        }

        // 노쇼 계산
        statsMap.values().forEach(sessionMap ->
                sessionMap.values().forEach(ticketMap ->
                        ticketMap.values().forEach(stat -> {
                            int noShow = stat.getReservations() - stat.getCheckins();
                            stat.setNoShows(Math.max(noShow, 0));
                        })
                )
        );

        // 플랫 리스트로 변환
        List<EventSessionStatistics> result = new ArrayList<>();
        statsMap.values().forEach(sessionMap ->
                sessionMap.values().forEach(ticketMap ->
                        result.addAll(ticketMap.values())
                )
        );

        return result;
    }
}
