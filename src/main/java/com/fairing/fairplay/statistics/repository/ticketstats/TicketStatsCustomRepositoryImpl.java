package com.fairing.fairplay.statistics.repository.ticketstats;

import com.fairing.fairplay.statistics.entity.reservation.EventTicketStatistics;
import com.fairing.fairplay.statistics.dto.reservation.DailyReservationRateDto;
import com.fairing.fairplay.statistics.dto.reservation.HourlyReservationRateDto;
import com.fairing.fairplay.reservation.entity.QReservation;
import com.fairing.fairplay.ticket.entity.QTicket;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPAExpressions;
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
                .where(
                r.event.eventId.eq(eventId)
                        .and(r.createdAt.goe(start))
                .and(r.createdAt.lt(end))
                )
        .groupBy(r.createdAt.hour())
                .fetch();

        // 전체 티켓 재고 조회
        Integer totalStock = queryFactory
                .select(t.stock.sum())
                .from(t)
                .where(t.ticketId.in(
                JPAExpressions
                        .select(r.ticket.ticketId).from(r)
                .where(r.event.eventId.eq(eventId))
                .distinct()
                ))
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
                .select(r.createdAt.year(), r.createdAt.month(), r.createdAt.dayOfMonth(), r.count())
                .from(r)
                .where(
                        r.event.eventId.eq(eventId)
                                .and(r.createdAt.goe(start))
                                .and(r.createdAt.lt(end))
                )
                .groupBy(r.createdAt.year(), r.createdAt.month(), r.createdAt.dayOfMonth())
                .orderBy(r.createdAt.year().asc(), r.createdAt.month().asc(), r.createdAt.dayOfMonth().asc())
                .fetch();

        // 전체 티켓 재고 조회
        Integer totalStock = queryFactory
                .select(t.stock.sum())
                .from(t)
                .join(r).on(t.ticketId.eq(r.ticket.ticketId))
                .where(r.event.eventId.eq(eventId))
                .fetchOne();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd");
        if (totalStock == null || totalStock == 0) {
            List<DailyReservationRateDto> zeros = new java.util.ArrayList<>();
            for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
                zeros.add(DailyReservationRateDto.builder()
                        .date(d.format(fmt))
                        .rate(0.0)
                        .build());
                }
            return zeros;
            }

        // 일별 예매율 계산
        java.util.Map<LocalDate, Long> countByDate = new java.util.HashMap<>();
        for (Tuple tuple : dailyReservations) {
            int year = tuple.get(r.createdAt.year());
            int month = tuple.get(r.createdAt.month());
            int day = tuple.get(r.createdAt.dayOfMonth());
            LocalDate d = LocalDate.of(year, month, day);
            countByDate.put(d, tuple.get(r.count()));
            }

                DateTimeFormatter fmts = DateTimeFormatter.ofPattern("MM/dd");
        java.util.List<DailyReservationRateDto> result = new java.util.ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            long reservationCount = countByDate.getOrDefault(d, 0L);
            double rate = ((double) reservationCount / totalStock) * 100;
            result.add(DailyReservationRateDto.builder()
                    .date(d.format(fmts))
                    .rate(Math.round(rate * 10.0) / 10.0)
                    .build());
            }
        return result;
    }
}
