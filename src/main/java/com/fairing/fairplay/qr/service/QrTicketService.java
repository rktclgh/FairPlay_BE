package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.attendee.entity.AttendeeTypeCode;
import com.fairing.fairplay.attendee.repository.AttendeeRepository;
import com.fairing.fairplay.attendee.repository.AttendeeTypeCodeRepository;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.qr.dto.QrTicketGuestResponseDto;
import com.fairing.fairplay.qr.dto.QrTicketReissueGuestRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketReissueMemberRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketReissueRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketReissueResponseDto;
import com.fairing.fairplay.qr.dto.QrTicketRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketResponseDto;
import com.fairing.fairplay.qr.dto.QrTicketUpdateResponseDto;
import com.fairing.fairplay.qr.entity.QrTicket;
import com.fairing.fairplay.qr.repository.QrTicketRepository;
import com.fairing.fairplay.qr.util.CodeGenerator;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import com.fairing.fairplay.ticket.entity.EventSchedule;
import com.fairing.fairplay.ticket.repository.EventScheduleRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*QR티켓 서비스*/
@Service
@RequiredArgsConstructor
@Slf4j
public class QrTicketService {

  private final QrTicketIssueService qrTicketIssueService;
  private final QrTicketRepository qrTicketRepository;
  private final AttendeeRepository attendeeRepository;
  private final EventScheduleRepository eventScheduleRepository;
  private final CodeGenerator codeGenerator;
  private final ReservationRepository reservationRepository;
  private final AttendeeTypeCodeRepository attendeeTypeCodeRepository;

  // 대표자/동반자 QR 티켓 생성
  public void generateQrTicket(Attendee attendee, Reservation reservation) {
    qrTicketIssueService.generateQrTicket(attendee, reservation);
  }

  // 회원 QR 티켓 조회 -> 마이페이지에서 조회
  @Transactional
  public QrTicketResponseDto issueMember(QrTicketRequestDto dto, CustomUserDetails userDetails) {
    return qrTicketIssueService.issueMemberTicket(dto, userDetails);
  }

  // 비회원 QR 티켓 조회 -> QR 티켓 링크 통한 조회
  @Transactional
  public QrTicketGuestResponseDto issueGuest(String token) {
    return qrTicketIssueService.issueGuestTicket(token);
  }

  /*
   * 재발급
   * 1. 사용자가 새로고침 버튼 클릭해 QR 코드 재생성
   * 2. 회원이 마이페이지에서 QR 링크 조회 안될 때 관리자 강제 QR 티켓 리셋
   * 3. 마이페이지 접근 안되는 회원/비회원에게 강제 QR 티켓 링크 재발급해 메일 전송
   * */
  // QR 티켓 재발급 1-1
  @Transactional
  public QrTicketUpdateResponseDto reissueQrTicketByGuest(QrTicketReissueGuestRequestDto dto) {
    return qrTicketIssueService.reissueQrTicketByGuest(dto);
  }

  // QR 티켓 재발급 1-2
  @Transactional
  public QrTicketUpdateResponseDto reissueQrTicketByMember(QrTicketReissueMemberRequestDto dto, CustomUserDetails userDetails) {
    return qrTicketIssueService.reissueQrTicketByMember(dto,userDetails);
  }

  // QR 티켓 재발급 2
  @Transactional
  public QrTicketReissueResponseDto reissueAdminQrTicketByUser(QrTicketReissueRequestDto dto) {
    return qrTicketIssueService.reissueAdminQrTicketByUser(dto);
  }

  // QR 티켓 재발급 3
  @Transactional
  public QrTicketReissueResponseDto reissueAdminQrTicket(QrTicketReissueRequestDto dto) {
    return qrTicketIssueService.reissueAdminQrTicket(dto);
  }

  // 테스트 강제 QR 티켓 발급
  @Transactional
  public void adminForceIssue(){

    EventSchedule es = eventScheduleRepository.findById(35L).orElse(null);
    Reservation re = reservationRepository.findById(23L).orElse(null);

    AttendeeTypeCode typeCode = attendeeTypeCodeRepository.findByCode(AttendeeTypeCode.PRIMARY).orElse(null);

    Attendee attendee = Attendee.builder()
        .name("김희연")
        .attendeeTypeCode(typeCode)
        .phone("01000000000")
        .email("khy3851@hanmail.net")
        .agreeToTerms(true)
        .reservation(re)
        .build();
    Attendee a = attendeeRepository.saveAndFlush(attendee);

    String eventCode = es.getEvent().getEventCode();
    LocalDateTime expiredAt = LocalDateTime.of(es.getDate(), es.getEndTime()); //만료날짜+시간 설정
    String ticketNo = codeGenerator.generateTicketNo(eventCode); // 티켓번호 설정

    QrTicket qrTicket = QrTicket.builder()
        .attendee(a)
        .eventSchedule(es)
        .reentryAllowed(false)
        .expiredAt(expiredAt)
        .issuedAt(LocalDateTime.now())
        .active(true)
        .ticketNo(ticketNo)
        .qrCode(null)
        .manualCode(null)
        .build();
    qrTicketRepository.save(qrTicket);

  }
}
