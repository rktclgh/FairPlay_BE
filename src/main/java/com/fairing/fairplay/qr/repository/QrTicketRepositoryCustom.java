package com.fairing.fairplay.qr.repository;

import com.fairing.fairplay.attendee.entity.QAttendee;
import com.fairing.fairplay.event.entity.QEvent;
import com.fairing.fairplay.event.entity.QEventDetail;
import com.fairing.fairplay.reservation.entity.QReservation;
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
public class QrTicketRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  public List<Tuple> findAllByEventDate(LocalDate targetDate) {
    QAttendee attendee = QAttendee.attendee;
    QReservation reservation = QReservation.reservation;
    QEvent event = QEvent.event;
    QEventDetail eventDetail = QEventDetail.eventDetail;
    QEventSchedule eventSchedule = QEventSchedule.eventSchedule;
    QScheduleTicket scheduleTicket = QScheduleTicket.scheduleTicket;

    // 참석자 정보, 이벤트 정보, 티켓 정보, 재입장 허용 여부, 스케줄 날짜+종료시간
    return queryFactory
        .select(attendee, event, reservation, eventDetail.reentryAllowed,
            eventSchedule)
        .from(attendee)
        .join(attendee.reservation, reservation) // 참석자가 예약한 정보 reservation
        .join(reservation.schedule, eventSchedule) // 예약과 연관된 행사 회차 정보 eventSchedule
        .join(eventSchedule.scheduleTickets, scheduleTicket) // 행사 회차에 판매되는 티켓 정보 집합 scheduleTicket
        .on(scheduleTicket.ticket.eq(reservation.ticket)) // 예약 티켓과 일치하는 schedule_ticket만 필터링
        .join(eventSchedule.event, event) // 행사 회차가 소속된 이벤트
        .join(event.eventDetail, eventDetail) // 이벤트에 대한 상세 정보
        .where(
            eventSchedule.date.eq(targetDate) // 조회 대상 날짜와 같은 이벤트만 필터링
        )
        .orderBy(reservation.reservationId.asc(), attendee.id.asc()) // 예약 ID와 참석자 ID 기준 결과 정렬
        .fetch();
  }
}
