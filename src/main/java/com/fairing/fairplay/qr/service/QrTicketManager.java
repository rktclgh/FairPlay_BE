package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.common.exception.LinkExpiredException;
import com.fairing.fairplay.qr.dto.QrTicketReissueRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketReissueResponseDto;
import com.fairing.fairplay.qr.dto.QrTicketRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketResponseDto;
import com.fairing.fairplay.qr.dto.QrTicketResponseDto.ViewingScheduleInfo;
import com.fairing.fairplay.qr.dto.QrTicketUpdateRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketUpdateResponseDto;
import com.fairing.fairplay.qr.entity.QrTicket;
import com.fairing.fairplay.qr.repository.QrTicketRepository;
import com.fairing.fairplay.qr.util.CodeGenerator;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// QR Ticket 발급 및 저장
@Component
@RequiredArgsConstructor
@Slf4j
public class QrTicketManager {

  private final QrTicketRepository qrTicketRepository;
  private final ReservationRepository reservationRepository;
  private final CodeGenerator codeGenerator;
  private final QrTicketInitProvider qrTicketInitProvider;
  private final QrLinkService qrLinkService;
  private final QrEmailService qrEmailService;

  // 회원 QR 티켓 조회 -> 마이페이지에서 조회
  @Transactional
  public QrTicketResponseDto issueMemberTicket(QrTicketRequestDto dto) {
    Reservation reservation = checkReservationBeforeNow(dto.getReservationId());

    QrTicket savedTicket = generateAndSaveQrTicket(dto, 1);
    return buildQrTicketResponse(savedTicket.getId(), reservation.getCreatedAt());
  }

  // 비회원 QR 티켓 조회 -> QR 티켓 링크 통한 조회
  @Transactional
  public QrTicketResponseDto issueGuestTicket(String token) {
    // 토큰 파싱해 예약 정보 조회
    QrTicketRequestDto dto = qrLinkService.decodeToDto(token);

    Reservation reservation = checkReservationBeforeNow(dto.getReservationId());

    // QR 티켓 조회해 qr code, manualcode 생성해서 반환
    QrTicket savedTicket = generateAndSaveQrTicket(dto, 2);
    // 프론트 응답
    return buildQrTicketResponse(savedTicket.getId(), reservation.getCreatedAt());
  }

  // QR 티켓 재발급 - 새로고침 버튼
  @Transactional
  public QrTicketUpdateResponseDto reissueQrTicket(QrTicketUpdateRequestDto dto) {
    // QR URL 디코딩
    QrTicketRequestDto qrTicketRequestDto = qrLinkService.decodeToDto(dto.getQrUrlToken());

    // 예약 여부 조회
    Reservation reservation = checkReservationBeforeNow(qrTicketRequestDto.getReservationId());

    // QR 티켓 재발급
    QrTicket savedTicket = generateAndSaveQrTicket(qrTicketRequestDto, null);
    return QrTicketUpdateResponseDto.builder().qrCode(savedTicket.getQrCode())
        .manualCode(savedTicket.getManualCode()).build();
  }

//  // 마이 페이지 강제 QR 티켓 재발급 - 행사 관리자
//  @Transactional
//  public QrTicketReissueResponseDto reissueByAdmin(QrTicketReissueRequestDto dto) {
//    QrTicket
//    // attendeeId 받음
//
//    // 참석자 ID 조회
//    Attendee attendee = attendeeRepository.findById(attendeeId);
//
//    // qr url 재발급
//    String qrUrl = qrLinkService.generateQrLink(dto);
//
//    // 메일 전송
//    qrEmailService.sendQrEmail(qrUrl, name, email);
//    // 메일 전송 성공 응답 - 메일 내용: 성공만 했다. url은 마이페이지에서 확인해라
//  }


  // 관리자 강제 QR 티켓 링크 재발급
  @Transactional
  public QrTicketReissueResponseDto reissueByAdmin(QrTicketReissueRequestDto dto) {
    QrTicket qrTicket = qrTicketRepository.findByTicketNo(dto.getTicketNo());

    // 재발급된 QR 티켓 - qrcode, manualcode null 처리
    QrTicket resetQrTicket = resetQrTicket(qrTicket);
    Attendee attendee = resetQrTicket.getAttendee();

    QrTicketRequestDto qrTicketRequestDto = QrTicketRequestDto.builder()
        .reservationId(resetQrTicket.getAttendee().getReservation().getReservationId())
        .eventId(resetQrTicket.getEventTicket().getEvent().getEventId())
        .ticketId(resetQrTicket.getEventTicket().getTicket().getTicketId())
        .attendeeId(resetQrTicket.getAttendee().getId())
        .build();

    // 메일 전송
    String qrUrl = qrLinkService.generateQrLink(qrTicketRequestDto);
    qrEmailService.sendQrEmail(qrUrl, attendee.getEmail(), attendee.getName());

    // 메일 전송 성공 응답 - 메일 내용: 성공했다. 메일로 qr url 보냇다.
    return buildQrTicketReissueResponse(resetQrTicket.getTicketNo(), attendee.getEmail());
  }

  // 행사일이 오늘보다 전날 또는 행사일이 오늘인데 종료 시간이 현재 시간보다 더 늦은 경우 예외 처리
  private Reservation checkReservationBeforeNow(Long reservationId) {
    Reservation reservation = reservationRepository.findById(reservationId)
        .orElseThrow(() -> new CustomException(
            HttpStatus.NOT_FOUND, "올바른 예약 티켓이 아닙니다."));

    LocalDate nowDate = LocalDate.now(ZoneId.of("Asia/Seoul"));
    LocalTime nowTime = LocalTime.now(ZoneId.of("Asia/Seoul"));

    if (reservation.getSchedule().getDate().isBefore(nowDate) || (
        reservation.getSchedule().getDate().isEqual(nowDate) && reservation.getSchedule()
            .getEndTime().isBefore(nowTime))) {
      throw new LinkExpiredException("종료된 행사입니다.", null);
    }

    return reservation;
  }

  // 저장된 qr 티켓 조회 후 qrcode, manualcode 발급받아 저장
  private QrTicket generateAndSaveQrTicket(QrTicketRequestDto dto, Integer type) {
    QrTicket qrTicket = findQrTicket(dto, type);
    qrTicket.setQrCode(codeGenerator.generateRandomToken());
    qrTicket.setManualCode(codeGenerator.generateManualCode());
    return qrTicketRepository.save(qrTicket);
  }

  private QrTicket resetQrTicket(QrTicket qrTicket) {
    qrTicket.setQrCode(null);
    qrTicket.setManualCode(null);
    return qrTicketRepository.save(qrTicket);
  }

  private QrTicket findQrTicket(QrTicketRequestDto dto, Integer type) {
    return qrTicketInitProvider.load(dto, type);
  }

  // 재발급 응답 생성
  private QrTicketReissueResponseDto buildQrTicketReissueResponse(String ticketNo, String email) {
    return QrTicketReissueResponseDto.builder()
        .ticketNo(ticketNo)
        .email(email)
        .build();
  }

  // 발급 응답 생성
  private QrTicketResponseDto buildQrTicketResponse(Long ticketId,
      LocalDateTime reservationCreatedAt) {
    QrTicketResponseDto dto = qrTicketRepository.findDtoById(ticketId)
        .orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "티켓 조회 실패"));

    if (reservationCreatedAt != null) {
      String formattedDate = reservationCreatedAt.format(
          DateTimeFormatter.ofPattern("yyyy. MM. dd"));
      dto.setReservationDate(formattedDate);
    }

    // 가상의 상영 정보 설정 예시
    ViewingScheduleInfo viewingScheduleInfo = getMockSchedule();
    dto.setViewingScheduleInfo(viewingScheduleInfo);
    return dto;
  }

  // 삭제 예정
  private ViewingScheduleInfo getMockSchedule() {
    return ViewingScheduleInfo.builder()
        .date("2025-08-01")
        .dayOfWeek("금")
        .startTime("14:00")
        .build();
  }

}
