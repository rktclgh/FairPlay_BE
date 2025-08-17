package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.attendee.entity.QAttendee;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.qr.dto.QrTicketRequestDto;
import com.fairing.fairplay.qr.entity.QrActionCode;
import com.fairing.fairplay.qr.entity.QrTicket;
import com.fairing.fairplay.qr.repository.QrTicketRepository;
import com.fairing.fairplay.qr.repository.QrTicketRepositoryCustom;
import com.fairing.fairplay.qr.util.CodeGenerator;
import com.fairing.fairplay.reservation.entity.QReservation;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.repository.ReservationRepositoryCustom;
import com.fairing.fairplay.ticket.entity.EventSchedule;

import com.fairing.fairplay.ticket.entity.QEventSchedule;
import com.querydsl.core.Tuple;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// QR티켓 스케줄러 관련 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class QrTicketBatchService {

  private final ReservationRepositoryCustom reservationRepositoryCustom;
  private final QrTicketRepository qrTicketRepository;
  private final QrTicketRepositoryCustom qrTicketRepositoryCustom;
  private final QrEmailService qrEmailService;
  private final QrLinkService qrLinkService;
  private final CodeGenerator codeGenerator;
  private final QrLogService qrLogService;
  private final QrEntryValidateService qrEntryValidateService;

  // 행사 1일 남은 예약건 조회
  public List<Tuple> fetchQrTicketBatch() {
    return reservationRepositoryCustom.findReservationsOneDayBeforeEventWithoutRepresentatives();
  }

  // 비회원 QR 티켓 링크 발급
  public void generateQrLink(List<Tuple> reservations) {
    QReservation reservation = QReservation.reservation;
    QAttendee attendee = QAttendee.attendee;
    QEventSchedule eventSchedule = QEventSchedule.eventSchedule;

    for (Tuple tuple : reservations) {
      Long reservationId = tuple.get(reservation.reservationId); // 예약 ID
      Long ticketId = tuple.get(reservation.ticket.ticketId); // 티켓 ID
      Long eventId = tuple.get(reservation.schedule.event.eventId); // 행사 ID
      Long attendeeId = tuple.get(attendee.id); //참석자 ID
      String attendeeName = tuple.get(attendee.name); // 참석자 이름
      String attendeeEmail = tuple.get(attendee.email); // 참석자 이메일
      EventSchedule schedule = tuple.get(eventSchedule);
      Event event = schedule.getEvent();

      String eventName = event.getTitleKr();
      String eventDate =
          getDate(event.getEventDetail().getStartDate()) + " ~ " + getDate(event.getEventDetail()
              .getEndDate());
      String viewingDate =
          schedule.getDate().toString() + " (" + getDayOfWeek(schedule.getWeekday()) + ") "
              + getTime(schedule.getStartTime());

      try {
        QrTicketRequestDto dto = QrTicketRequestDto.builder()
            .attendeeId(attendeeId)
            .reservationId(reservationId)
            .eventId(eventId)
            .ticketId(ticketId)
            .build();
        String qrUrl = qrLinkService.generateQrLink(dto);
        qrEmailService.sendQrEmail(qrUrl, eventName, eventDate, viewingDate, attendeeEmail, attendeeName);
        log.info("이메일 전송 완료:{}", attendeeEmail);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  // QR 티켓 엔티티 생성 - 스케쥴러가 실행
  @Transactional
  public void createQrTicket() {
    List<QrTicket> qrTickets = scheduleCreateQrTicket();

    // 발급할 티켓이 없을 경우
    if (qrTickets == null || qrTickets.isEmpty()) {
      return;
    }

    qrTicketRepository.saveAll(qrTickets);
    qrTicketRepository.flush();

    List<Long> ticketIds = qrTickets.stream()
        .map(QrTicket::getId)
        .collect(Collectors.toList());

    List<QrTicket> persistedTickets = qrTicketRepository.findAllById(ticketIds);
    log.info("🚩 persistedTickets 생성됨: {}", persistedTickets.size());
    QrActionCode qrActionCode = qrEntryValidateService.validateQrActionCode(QrActionCode.ISSUED);
    log.info("🚩 qrActionCode: {}", qrActionCode.getCode());
    qrLogService.issuedQrLog(persistedTickets, qrActionCode);
  }

  /*
   * 다음날 열리는 행사에 참석 예정인 모든 사람을 조회해
   * 각 참석자에 대한 QR티켓을 발급 ( QR코드, 수동코드는 생성 안함 )
   * */
  private List<QrTicket> scheduleCreateQrTicket() {
    // 현재 날짜 기준 다음날 시작하는 행사 데이터 추출
    LocalDate targetDate = LocalDate.now().plusDays(1);

    log.info("[QrTicketInitProvider] scheduleCreateQrTicket - targetDate: {}", targetDate);

    List<Tuple> results = qrTicketRepositoryCustom.findAllByEventDate(targetDate);

    // attendeeId, reservationId 집합 추출
    Set<Long> attendeeIds = results.stream()
        .map(tuple -> tuple.get(0, Attendee.class).getId())
        .collect(Collectors.toSet());

    Set<Long> reservationIds = results.stream()
        .map(tuple -> tuple.get(2, Reservation.class).getReservationId())
        .collect(Collectors.toSet());

    // 이미 발급된 티켓을 한 번에 조회
    Set<String> issuedTicketKeys = qrTicketRepository
        .findByAttendeeIdsAndReservationIds(attendeeIds, reservationIds)
        .stream()
        .map(ticket -> ticket.getAttendee().getId() + "_" + ticket.getAttendee().getReservation()
            .getReservationId())
        .collect(Collectors.toSet());

    return results.stream()
        .filter(tuple -> {
          Attendee a = tuple.get(0, Attendee.class);
          Reservation r = tuple.get(2, Reservation.class);
          String key = a.getId() + "_" + r.getReservationId();
          return !issuedTicketKeys.contains(key);
        })
        .map(tuple -> {
          Attendee a = tuple.get(0, Attendee.class);
          Event e = tuple.get(1, Event.class);
          Reservation r = tuple.get(2, Reservation.class);
          Boolean reentryAllowed = tuple.get(3, Boolean.class);
          EventSchedule es = tuple.get(4, EventSchedule.class);

          if (a == null || e == null || r == null || reentryAllowed == null || es == null) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "엔티티가 조회되지 않았습니다.");
          }

          String eventCode = e.getEventCode();
          LocalDateTime expiredAt = LocalDateTime.of(es.getDate(), es.getEndTime()); //만료날짜+시간 설정
          String ticketNo = codeGenerator.generateTicketNo(eventCode); // 티켓번호 설정

          return QrTicket.builder()
              .attendee(a)
              .eventSchedule(es)
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

  private String getDate(LocalDate date) {
    return date.format(DateTimeFormatter.ofPattern("yyyy. MM. dd"));
  }

  private String getDayOfWeek(int weekday) {
    int dayOfWeekValue = (weekday == 0) ? 7 : weekday; // 0(일)은 7로 매핑
    return DayOfWeek.of(dayOfWeekValue).getDisplayName(TextStyle.FULL, Locale.KOREAN);
  }

  private String getTime(LocalTime time) {
    return time.format(DateTimeFormatter.ofPattern("HH:mm"));
  }
}
