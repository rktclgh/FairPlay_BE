package com.fairing.fairplay.statistics.repository.salesstats;

import com.fairing.fairplay.payment.entity.QPaymentStatusCode;
import com.fairing.fairplay.reservation.entity.QReservation;
import com.fairing.fairplay.statistics.dto.sales.RawSalesData;
import com.fairing.fairplay.ticket.entity.QEventSchedule;
import com.fairing.fairplay.ticket.entity.QTicket;
import com.fairing.fairplay.payment.entity.QPayment;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SalesRawDataRepositoryImpl implements SalesRawDataRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<RawSalesData> fetchSalesData(LocalDate targetDate) {
        QPayment p = QPayment.payment;
        QReservation r = QReservation.reservation;
        QEventSchedule s = QEventSchedule.eventSchedule;
        QTicket t = QTicket.ticket;
        QPaymentStatusCode ps = QPaymentStatusCode.paymentStatusCode;

        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

        return queryFactory
                .select(
                        r.event.eventId,
                        r.schedule.scheduleId,
                        t.name,
                        t.price,
                        r.quantity,
                        p.amount,
                        ps.code
                )
                .from(p)
                .join(r).on(p.reservation.reservationId.eq(r.reservationId))
                .join(s).on(r.schedule.scheduleId.eq(s.scheduleId))
                .join(t).on(r.ticket.ticketId.eq(t.ticketId))
                .join(ps).on(p.paymentStatusCode.paymentStatusCodeId.eq(ps.paymentStatusCodeId))
                .where(p.paidAt.between(start, end))
                .fetch()
                .stream()
                .map(row -> RawSalesData.builder()
                        .eventId(row.get(r.event.eventId))
                        .scheduleId(row.get(r.schedule.scheduleId))
                        .ticketName(row.get(t.name))
                        .unitPrice(row.get(t.price).longValue())
                        .quantity(row.get(r.quantity))
                        .amount(row.get(p.amount).longValue())
                        .paymentStatus(row.get(ps.code))
                        .build()
                )
                .toList();
    }

    @Override
    public List<RawSalesData> fetchSalesDataChunk(LocalDate targetDate, int offset, int pageSize) {
        QPayment p = QPayment.payment;
        QReservation r = QReservation.reservation;
        QEventSchedule s = QEventSchedule.eventSchedule;
        QTicket t = QTicket.ticket;
        QPaymentStatusCode ps = QPaymentStatusCode.paymentStatusCode;

        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

        return queryFactory
                .select(
                        r.event.eventId,
                        r.schedule.scheduleId,
                        t.name,
                        t.price,
                        r.quantity,
                        p.amount,
                        ps.code
                )
                .from(p)
                .join(r).on(p.reservation.reservationId.eq(r.reservationId))
                .join(s).on(r.schedule.scheduleId.eq(s.scheduleId))
                .join(t).on(r.ticket.ticketId.eq(t.ticketId))
                .join(ps).on(p.paymentStatusCode.paymentStatusCodeId.eq(ps.paymentStatusCodeId))
                .where(p.paidAt.between(start, end))
                .orderBy(p.paymentId.asc()) // 일관된 정렬을 위해 추가
                .offset(offset)
                .limit(pageSize)
                .fetch()
                .stream()
                .map(row -> RawSalesData.builder()
                        .eventId(row.get(r.event.eventId))
                        .scheduleId(row.get(r.schedule.scheduleId))
                        .ticketName(row.get(t.name))
                        .unitPrice(row.get(t.price).longValue())
                        .quantity(row.get(r.quantity))
                        .amount(row.get(p.amount).longValue())
                        .paymentStatus(row.get(ps.code))
                        .build()
                )
                .toList();
    }

    // 전체 데이터 개수를 조회하는 메서드 (선택사항)
    @Override
    public long countSalesData(LocalDate targetDate) {
        QPayment p = QPayment.payment;
        QReservation r = QReservation.reservation;
        QEventSchedule s = QEventSchedule.eventSchedule;
        QTicket t = QTicket.ticket;
        QPaymentStatusCode ps = QPaymentStatusCode.paymentStatusCode;

        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

        return queryFactory
                .select(p.count())
                .from(p)
                .join(r).on(p.reservation.reservationId.eq(r.reservationId))
                .join(s).on(r.schedule.scheduleId.eq(s.scheduleId))
                .join(t).on(r.ticket.ticketId.eq(t.ticketId))
                .join(ps).on(p.paymentStatusCode.paymentStatusCodeId.eq(ps.paymentStatusCodeId))
                .where(p.paidAt.between(start, end))
                .fetchOne();
    }
}