package com.fairing.fairplay.reservation.repository;

import com.fairing.fairplay.attendee.entity.AttendeeTypeCode;
import com.fairing.fairplay.attendee.entity.QAttendee;
import com.fairing.fairplay.attendee.repository.AttendeeTypeCodeRepository;
import com.fairing.fairplay.reservation.entity.QReservation;
import com.fairing.fairplay.reservation.entity.ReservationStatusCode;
import com.fairing.fairplay.ticket.entity.QEventSchedule;
import com.fairing.fairplay.ticket.entity.QScheduleTicket;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationRepositoryCustom {

  public static final String ATTENDEE_TYPE_GUEST = "GUEST";
  public static final String RESERVATION_STATUS_CONFIRMED = "CONFIRMED";
  private final JPAQueryFactory queryFactory;
  private final AttendeeTypeCodeRepository attendeeTypeCodeRepository;

  // 내일 일정이고, 확정된 예약이며, 취소되지 않았고, 게스트 참석자가 있는 예약 정보
  public List<Tuple> findReservationsOneDayBeforeEventWithoutRepresentatives() {
    LocalDate tomorrow = LocalDate.now().plusDays(1);
    log.info(
        "[ReservationRepositoryCustom] findReservationsOneDayBeforeEventWithoutRepresentatives = {}",
        tomorrow);

    AttendeeTypeCode attendeeTypeCode = attendeeTypeCodeRepository.findByCode("GUEST").orElseThrow(
        () -> new IllegalArgumentException("올바르지 않은 참석자 코드입니다.")
    );

    QReservation reservation = QReservation.reservation;
    QEventSchedule schedule = QEventSchedule.eventSchedule;
    QScheduleTicket scheduleTicket = QScheduleTicket.scheduleTicket;
    QAttendee attendee = QAttendee.attendee;

    List<Tuple> results = queryFactory
        .select(
            reservation.reservationId,
            reservation.ticket.ticketId,
            attendee.id,
            attendee.name,
            attendee.email,
            reservation.event.eventId)
        .from(reservation)
        .join(schedule).on(schedule.scheduleId.eq(reservation.schedule.scheduleId))
        .join(scheduleTicket).on(
            scheduleTicket.id.scheduleId.eq(schedule.scheduleId)
                .and(scheduleTicket.id.ticketId.eq(reservation.ticket.ticketId))
        )
        .join(attendee).on(
            attendee.reservation.eq(reservation)
                .and(attendee.attendeeTypeCode.id.eq(attendeeTypeCode.getId()))
        )
        .where(
            schedule.date.eq(tomorrow),
            reservation.canceled.isFalse(),
            reservation.reservationStatusCode.code.eq(RESERVATION_STATUS_CONFIRMED)
        )
        .fetch();

    log.info(
        "[ReservationRepositoryCustom] findReservationsOneDayBeforeEventWithoutRepresentatives results:{}",
        results.size());

    return results;
  }
}
