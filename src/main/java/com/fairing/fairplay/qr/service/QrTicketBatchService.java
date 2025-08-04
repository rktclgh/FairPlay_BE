package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.attendee.entity.QAttendee;
import com.fairing.fairplay.core.email.entity.EmailServiceFactory;
import com.fairing.fairplay.core.email.entity.EmailServiceFactory.EmailType;
import com.fairing.fairplay.qr.dto.QrTicketRequestDto;
import com.fairing.fairplay.qr.util.QrLinkTokenGenerator;
import com.fairing.fairplay.reservation.entity.QReservation;
import com.fairing.fairplay.reservation.repository.ReservationRepositoryCustom;
import com.fairing.fairplay.ticket.entity.QEventSchedule;
import com.querydsl.core.Tuple;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// QR티켓 스케줄러 관련 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class QrTicketBatchService {

  private final QrLinkTokenGenerator qrLinkTokenGenerator;
  private final EmailServiceFactory emailServiceFactory;
  private final ReservationRepositoryCustom reservationRepositoryCustom;

  // 행사 1일 남은 예약건 조회
  public List<Tuple> fetchQrTicketBatch() {
    return reservationRepositoryCustom.findReservationsOneDayBeforeEventWithoutRepresentatives();
  }

  // 비회원 QR 티켓 링크 발급 -> 스케쥴러가 실행. 오전9시 실행 batch 도입 예정
  public void generateQrLink(List<Tuple> reservations) {
    QReservation reservation = QReservation.reservation;
    QAttendee attendee = QAttendee.attendee;
    QEventSchedule schedule = QEventSchedule.eventSchedule;

    for (Tuple tuple : reservations) {
      Long reservationId = tuple.get(reservation.reservationId);
      Long ticketId = tuple.get(reservation.ticket.ticketId);
      Long attendeeId = tuple.get(attendee.id);
      String attendeeName = tuple.get(attendee.name);
      String attendeeEmail = tuple.get(attendee.email);
      Long eventId = tuple.get(schedule.event.eventId);

      try {
        QrTicketRequestDto dto = QrTicketRequestDto.builder()
            .attendeeId(attendeeId)
            .reservationId(reservationId)
            .eventId(eventId)
            .ticketId(ticketId)
            .build();
        String token = qrLinkTokenGenerator.generateToken(dto);
        String qrUrl = "https://your-site.com/qr-tickets/" + token; // 수정 예정
        emailServiceFactory.getService(EmailType.QR_TICKET)
            .send(attendeeEmail, attendeeName, qrUrl);
        log.info("이메일 전송 완료:{}", attendeeEmail);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
