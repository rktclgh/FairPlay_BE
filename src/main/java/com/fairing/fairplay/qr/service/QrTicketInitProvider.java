package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.attendee.entity.QAttendee;
import com.fairing.fairplay.attendee.repository.AttendeeRepository;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.entity.QEvent;
import com.fairing.fairplay.event.entity.QEventDetail;
import com.fairing.fairplay.qr.dto.QrTicketRequestDto;
import com.fairing.fairplay.qr.entity.QrTicket;
import com.fairing.fairplay.qr.repository.QrTicketRepository;
import com.fairing.fairplay.qr.util.CodeGenerator;
import com.fairing.fairplay.reservation.entity.QReservation;
import com.fairing.fairplay.ticket.entity.EventTicket;
import com.fairing.fairplay.ticket.entity.QEventSchedule;
import com.fairing.fairplay.ticket.entity.QTicket;
import com.fairing.fairplay.ticket.entity.Ticket;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/*
 * QR 티켓 객체 생성 클래스
 * */
@Component
@RequiredArgsConstructor
public class QrTicketInitProvider {

  private final AttendeeRepository attendeeRepository;
  private final QrTicketRepository qrTicketRepository;
  private final JPAQueryFactory queryFactory;
  private final CodeGenerator codeGenerator;

  public QrTicket load(QrTicketRequestDto dto, Integer attendeeTypeCodeId) {
    Attendee attendee;

    if (attendeeTypeCodeId == 1) {
      // 대표자: 예약 ID + 타입으로 한 명만 조회
      attendee = attendeeRepository.findByReservation_ReservationIdAndAttendeeTypeCode_Id(
              dto.getReservationId(), attendeeTypeCodeId)
          .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "참석자를 조회할 수 없습니다."));

    } else if (attendeeTypeCodeId == 2) {
      // 동반자: 예약 ID + 참석자 ID + 타입으로 조회
      if (dto.getAttendeeId() == null) {
        throw new CustomException(HttpStatus.BAD_REQUEST, "대표자가 아니므로 조회할 수 없습니다.");
      }

      attendee = attendeeRepository.findByIdAndReservation_ReservationIdAndAttendeeTypeCode_Id(
              dto.getAttendeeId(), dto.getReservationId(), attendeeTypeCodeId)
          .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "동반 참석자를 조회할 수 없습니다."));

    } else {
      throw new CustomException(HttpStatus.BAD_REQUEST, "지원하지 않는 참석자 유형입니다.");
    }

    return qrTicketRepository.findByAttendee(attendee)
        .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "QR 티켓을 조회할 수 없습니다."));
  }

  /*
   * 다음날 열리는 행사에 참석 예정인 모든 사람을 조회해
   * 각 참석자에 대한 QR티켓을 발급 ( QR코드는 생성 안함 )
   * */
  public List<QrTicket> scheduleCreateQrTicket() {

    // 현재 날짜 기준 다음날 시작하는 행사 데이터 추출
    LocalDate targetDate = LocalDate.now().plusDays(1);

    QAttendee attendee = QAttendee.attendee;
    QReservation reservation = QReservation.reservation;
    QEvent event = QEvent.event;
    QEventDetail eventDetail = QEventDetail.eventDetail;
    QEventSchedule eventSchedule = QEventSchedule.eventSchedule;
    QTicket ticket = QTicket.ticket;

    // 참석자 정보, 이벤트 정보, 티켓 정보, 재입장 허용 여부, 스케줄 날짜+종료시간
    List<Tuple> results = queryFactory
        .select(attendee,
            event,
            ticket,
            eventDetail.reentryAllowed,
            eventSchedule.date,
            eventSchedule.endTime)
        .from(attendee)
        .join(attendee.reservation, reservation)
        .join(reservation.schedule, eventSchedule)
        .join(eventSchedule.event, event)
        .join(event.eventDetail, eventDetail)
        .where(eventDetail.startDate.eq(targetDate))
        .fetch();

    return results.stream()
        .map(tuple -> {
          Attendee a = tuple.get(attendee);
          Event e = tuple.get(event);
          Ticket t = tuple.get(ticket);
          Boolean reentryAllowed = tuple.get(eventDetail.reentryAllowed);
          LocalDate date = tuple.get(eventSchedule.date);
          LocalTime endTime = tuple.get(eventSchedule.endTime);
          String eventCode = tuple.get(event.eventCode);

          LocalDateTime expiredAt = LocalDateTime.of(date, endTime); //만료시간 설정
          String ticketNo = codeGenerator.generateTicketNo(eventCode); // 티켓번호 설정

          return QrTicket.builder()
              .attendee(a)
              .eventTicket(new EventTicket(t, e))
              .reentryAllowed(reentryAllowed)
              .expiredAt(expiredAt)
              .issuedAt(LocalDateTime.now())
              .active(true)
              .ticketNo(ticketNo)
              .qrCode(null)
              .manualCode(null)
              .build();
        })
        .toList();
  }
}
