package com.fairing.fairplay.reservation.repository;

import com.fairing.fairplay.attendee.entity.QAttendee;
import com.fairing.fairplay.reservation.entity.QReservation;
import com.fairing.fairplay.ticket.entity.QEventSchedule;
import com.fairing.fairplay.ticket.entity.QScheduleTicket;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  // 행사 하루 전 예약건의 참석자 중 대표자가 아닌 사용자 조회
  public List<Tuple> findReservationsOneDayBeforeEventWithoutRepresentatives() {
    LocalDate tomorrow = LocalDate.now().plusDays(1);

    QReservation reservation = QReservation.reservation;
    QEventSchedule schedule = QEventSchedule.eventSchedule;
    QScheduleTicket scheduleTicket = QScheduleTicket.scheduleTicket;
    QAttendee attendee = QAttendee.attendee;

    return queryFactory
        .select(reservation.reservationId, reservation.ticket.ticketId, attendee.id, attendee.name, attendee.email,
            schedule.event.eventId)
        .from(reservation)
        .join(schedule).on(reservation.schedule.scheduleId.eq(schedule.scheduleId))
        .join(scheduleTicket).on(schedule.scheduleId.eq(scheduleTicket.id.scheduleId)
            .and(reservation.ticket.ticketId.eq(scheduleTicket.id.ticketId)))
        .join(attendee).on(attendee.reservation.eq(reservation)
            .and(attendee.attendeeTypeCode.code.eq("GUEST")))  // 대표자 아님
        .where(
            schedule.date.eq(tomorrow),
            reservation.canceled.isFalse(),
            reservation.reservationStatusCode.code.eq("CONFIRMED") // 예시
        )
        .fetch();
  }
}
