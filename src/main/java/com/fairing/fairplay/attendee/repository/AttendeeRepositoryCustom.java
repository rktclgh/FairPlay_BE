package com.fairing.fairplay.attendee.repository;

import com.fairing.fairplay.attendee.entity.QAttendee;
import com.fairing.fairplay.event.entity.QEvent;
import com.fairing.fairplay.event.entity.QEventDetail;
import com.fairing.fairplay.reservation.entity.QReservation;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AttendeeRepositoryCustom {

  private final JPAQueryFactory jpaQueryFactory;

  public void deleteAttendeesByEventEndDate() {
    QAttendee attendee = QAttendee.attendee;
    QReservation reservation = QReservation.reservation;
    QEvent event = QEvent.event;
    QEventDetail eventDetail = QEventDetail.eventDetail;

    LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);

    long deletedCount = jpaQueryFactory
        .delete(attendee)
        .where(attendee.reservation.event.eventDetail.endDate.before(LocalDate.from(sixMonthsAgo)))
        .execute();

    System.out.println("[Attendee Cleanup] 삭제된 참석자 수: " + deletedCount);
  }
}
