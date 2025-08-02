package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.qr.dto.QrTicketRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketResponseDto;
import com.fairing.fairplay.qr.dto.QrTicketResponseDto.ViewingScheduleInfo;
import com.fairing.fairplay.qr.entity.QrTicket;
import com.fairing.fairplay.qr.repository.QrTicketRepository;
import com.fairing.fairplay.qr.util.CodeGenerator;
import com.fairing.fairplay.qr.util.QrLinkTokenGenerator;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*QR티켓 서비스*/
@Service
@RequiredArgsConstructor
public class QrTicketService {

  private final QrTicketRepository qrTicketRepository;
  private final ReservationRepository reservationRepository;
  private final QrLinkTokenGenerator qrLinkTokenGenerator;
  private final CodeGenerator codeGenerator;
  private final QrTicketInitProvider qrTicketInitProvider;

  // 회원 QR 티켓 조회 -> 마이페이지에서 조회
  @Transactional
  public QrTicketResponseDto issueMember(QrTicketRequestDto dto) {
    Reservation reservation = reservationRepository.findById(dto.getReservationId())
        .orElseThrow(() -> new CustomException(
            HttpStatus.NOT_FOUND, "올바른 예약 티켓이 아닙니다."));
    QrTicket savedTicket = generateAndSaveQrTicket(dto, 1);
    return buildQrTicketResponse(savedTicket.getId(), reservation.getCreatedAt());
  }

  // 비회원 QR 티켓 조회 -> QR 티켓 링크 통한 조회
  @Transactional
  public QrTicketResponseDto issueGuest(String token) {
    // 토큰 파싱해 예약 정보 조회
    QrTicketRequestDto dto = qrLinkTokenGenerator.decodeToDto(token);
    Reservation reservation = reservationRepository.findById(dto.getReservationId())
        .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "올바른 예약 티켓이 아닙니다."));
    // QR 티켓 조회해 qr code, manualcode 생성해서 반환
    QrTicket savedTicket = generateAndSaveQrTicket(dto, 2);
    // 프론트 응답
    return buildQrTicketResponse(savedTicket.getId(), reservation.getCreatedAt());
  }

  // QR 티켓 엔티티 생성 - 스케쥴러가 실행
  @Transactional
  public void createQrTicket() {
    List<QrTicket> qrTickets = qrTicketInitProvider.scheduleCreateQrTicket();
    qrTicketRepository.saveAll(qrTickets);
  }

  // 저장된 qr 티켓 조회 후 qrcode, manualcode 발급받아 저장
  private QrTicket generateAndSaveQrTicket(QrTicketRequestDto dto, int type) {
    QrTicket qrTicket = qrTicketInitProvider.load(dto, type);
    qrTicket.setQrCode(codeGenerator.generateRandomToken());
    qrTicket.setManualCode(codeGenerator.generateManualCode());
    return qrTicketRepository.save(qrTicket);
  }

  // 프론트 응답 설정
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

  private ViewingScheduleInfo getMockSchedule() {
    return ViewingScheduleInfo.builder()
        .date("2025-08-01")
        .dayOfWeek("금")
        .startTime("14:00")
        .build();
  }
}
