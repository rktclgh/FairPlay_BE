package com.fairing.fairplay.statistics.repository.ticketstats;

import com.fairing.fairplay.statistics.entity.reservation.EventTicketStatistics;
import com.fairing.fairplay.statistics.dto.reservation.DailyReservationRateDto;
import com.fairing.fairplay.statistics.dto.reservation.HourlyReservationRateDto;
import com.fairing.fairplay.reservation.entity.QReservation;
import com.fairing.fairplay.ticket.entity.QTicket;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.IntStream;

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
                .select(r.event.eventId, t.name, r.count(), t.stock )
                .from(r)
                .join(t).on(r.ticket.ticketId.eq(t.ticketId))
                .where(r.createdAt.between(start, end))
                .groupBy(r.event.eventId, t.name)
                .fetch();

        return results.stream()
                .map(tuple -> EventTicketStatistics.builder()
                        .eventId(tuple.get(r.event.eventId))
                        .statDate(targetDate)
                        .stock(tuple.get(t.stock))
                        .ticketType(tuple.get(t.name))
                        .reservations(tuple.get(r.count()).intValue())
                        .createdAt(LocalDateTime.now())
                        .build())
                .toList();
    }

    @Override
    public List<HourlyReservationRateDto> calculateHourlyReservationRate(Long eventId, LocalDate startDate, LocalDate endDate) {
        QReservation r = QReservation.reservation;
        QTicket t = QTicket.ticket;

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        // 시간별 예매 건수 조회
        List<Tuple> hourlyReservations = queryFactory
                .select(r.createdAt.hour(), r.count())
                .from(r)
                .join(t).on(r.ticket.ticketId.eq(t.ticketId))
                .where(
                        r.event.eventId.eq(eventId)
                                .and(r.createdAt.between(start, end))
                )
                .groupBy(r.createdAt.hour())
                .fetch();

        // 전체 티켓 재고 조회
        Integer totalStock = queryFactory
                .select(t.stock.sum())
                .from(t)
                .join(r).on(t.ticketId.eq(r.ticket.ticketId))
                .where(r.event.eventId.eq(eventId))
                .fetchOne();

        if (totalStock == null || totalStock == 0) {
            return IntStream.range(0, 24)
                    .mapToObj(hour -> HourlyReservationRateDto.builder()
                            .time(String.format("%02d:00", hour))
                            .rate(0.0)
                            .build())
                    .toList();
        }

        // 시간별 예매율 계산
        return IntStream.range(0, 24)
                .mapToObj(hour -> {
                    Long reservationCount = hourlyReservations.stream()
                            .filter(tuple -> tuple.get(r.createdAt.hour()).equals(hour))
                            .mapToLong(tuple -> tuple.get(r.count()))
                            .findFirst()
                            .orElse(0L);

                    double rate = (double) reservationCount / totalStock * 100;

                    return HourlyReservationRateDto.builder()
                            .time(String.format("%02d:00", hour))
                            .rate(Math.round(rate * 10.0) / 10.0)
                            .build();
                })
                .toList();
    }

    @Override
    public List<DailyReservationRateDto> calculateDailyReservationRate(Long eventId, LocalDate startDate, LocalDate endDate) {
        QReservation r = QReservation.reservation;
        QTicket t = QTicket.ticket;

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        // 일별 예매 건수 조회
        List<Tuple> dailyReservations = queryFactory
                .select(r.createdAt.dayOfMonth(), r.createdAt.month(), r.count())
                .from(r)
                .join(t).on(r.ticket.ticketId.eq(t.ticketId))
                .where(
                        r.event.eventId.eq(eventId)
                                .and(r.createdAt.between(start, end))
                )
                .groupBy(r.createdAt.dayOfMonth(), r.createdAt.month())
                .orderBy(r.createdAt.month().asc(), r.createdAt.dayOfMonth().asc())
                .fetch();

        // 전체 티켓 재고 조회
        Integer totalStock = queryFactory
                .select(t.stock.sum())
                .from(t)
                .join(r).on(t.ticketId.eq(r.ticket.ticketId))
                .where(r.event.eventId.eq(eventId))
                .fetchOne();

        if (totalStock == null || totalStock == 0) {
            return dailyReservations.stream()
                    .map(tuple -> {
                        Integer month = tuple.get(r.createdAt.month());
                        Integer day = tuple.get(r.createdAt.dayOfMonth());
                        return DailyReservationRateDto.builder()
                                .date(String.format("%02d/%02d", month, day))
                                .rate(0.0)
                                .build();
                    })
                    .toList();
        }

        // 일별 예매율 계산
        return dailyReservations.stream()
                .map(tuple -> {
                    Integer month = tuple.get(r.createdAt.month());
                    Integer day = tuple.get(r.createdAt.dayOfMonth());
                    Long reservationCount = tuple.get(r.count());

                    double rate = (double) reservationCount / totalStock * 100;

                    return DailyReservationRateDto.builder()
                            .date(String.format("%02d/%02d", month, day))
                            .rate(Math.round(rate * 10.0) / 10.0)
                            .build();
                })
                .toList();
    }
}
