package com.fairing.fairplay.qr.repository;

import com.fairing.fairplay.attendee.entity.QAttendee;
import com.fairing.fairplay.event.entity.QEvent;
import com.fairing.fairplay.event.entity.QEventDetail;
import com.fairing.fairplay.reservation.entity.QReservation;
import com.fairing.fairplay.ticket.entity.QEventSchedule;
import com.fairing.fairplay.ticket.entity.QTicket;
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

  public List<Tuple> findAllByEventDate(LocalDate targetDate){
    QAttendee attendee = QAttendee.attendee;
    QReservation reservation = QReservation.reservation;
    QEvent event = QEvent.event;
    QEventDetail eventDetail = QEventDetail.eventDetail;
    QEventSchedule eventSchedule = QEventSchedule.eventSchedule;
    QTicket ticket = QTicket.ticket;

    // 참석자 정보, 이벤트 정보, 티켓 정보, 재입장 허용 여부, 스케줄 날짜+종료시간
    return queryFactory
        .select(attendee, event, ticket, reservation, eventDetail.reentryAllowed,
            eventSchedule.date, eventSchedule.startTime, eventSchedule.endTime)
        .from(attendee)
        .join(attendee.reservation, reservation)
        .join(reservation.schedule, eventSchedule)
        .join(eventSchedule.event, event)
        .join(reservation.ticket, ticket)
        .join(event.eventDetail, eventDetail)
        .where(
            eventDetail.startDate.eq(targetDate)
        )
        .fetch();
  }
}
