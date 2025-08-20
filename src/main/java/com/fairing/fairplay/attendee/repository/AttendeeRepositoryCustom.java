package com.fairing.fairplay.attendee.repository;

import com.fairing.fairplay.attendee.entity.QAttendee;
import com.fairing.fairplay.event.entity.QEvent;
import com.fairing.fairplay.event.entity.QEventDetail;
import com.fairing.fairplay.reservation.entity.QReservation;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AttendeeRepositoryCustom {

  private final JPAQueryFactory jpaQueryFactory;
  private final EntityManager entityManager;

  public void deleteAttendeesByEventEndDate() {
    QAttendee attendee = QAttendee.attendee;
    QReservation reservation = QReservation.reservation;
    QEvent event = QEvent.event;
    QEventDetail eventDetail = QEventDetail.eventDetail;

    LocalDate sixMonthsAgo = LocalDate.now().minusMonths(12);

    long deletedCount = jpaQueryFactory
        .delete(attendee)
        .where(attendee.reservation.reservationId.in(
            JPAExpressions.select(reservation.reservationId)
                .from(reservation)
                .join(reservation.event, event)
                .join(event.eventDetail, eventDetail)
                .where(eventDetail.endDate.before(sixMonthsAgo))
        ))
        .execute();

    entityManager.clear();
    log.info("[Attendee Cleanup] 삭제된 참석자 수: {}", deletedCount);
  }
}
