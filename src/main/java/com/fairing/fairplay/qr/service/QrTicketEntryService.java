package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.attendee.entity.AttendeeTypeCode;
import com.fairing.fairplay.attendee.repository.AttendeeRepository;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.qr.dto.CheckInResponseDto;
import com.fairing.fairplay.qr.dto.GuestManualCheckInRequestDto;
import com.fairing.fairplay.qr.dto.GuestQrCheckInRequestDto;
import com.fairing.fairplay.qr.dto.MemberManualCheckInRequestDto;
import com.fairing.fairplay.qr.dto.MemberQrCheckInRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketRequestDto;
import com.fairing.fairplay.qr.entity.QrCheckLog;
import com.fairing.fairplay.qr.entity.QrLog;
import com.fairing.fairplay.qr.entity.QrTicket;
import com.fairing.fairplay.qr.repository.QrCheckLogRepository;
import com.fairing.fairplay.qr.repository.QrLogRepository;
import com.fairing.fairplay.qr.repository.QrTicketRepository;
import com.fairing.fairplay.qr.util.CodeValidator;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

// QR 티켓 입장/퇴장
@Service
@RequiredArgsConstructor
public class QrTicketEntryService {
  private final QrTicketRepository qrTicketRepository;
  private final AttendeeRepository attendeeRepository;
  private final CodeValidator codeValidator; // 검증 로직 분리
  private final QrLogRepository qrLogRepository;
  private final QrCheckLogRepository qrCheckLogRepository;
  private final QrLogService qrLogService;

  // 회원 QR 코드 체크인
  public CheckInResponseDto checkIn(MemberQrCheckInRequestDto dto) {
    // 예약자 조회
    Attendee attendee = findAttendeeByReservation(dto.getReservationId());
    // QR 티켓 조회
    QrTicket qrTicket = findQrTicket(attendee);
    // qrcode 비교
    verifyQrCode(qrTicket, dto.getQrCode());
    // 재입장 여부 검토
    verifyReEntry(qrTicket);
    // QR 티켓 처리
    LocalDateTime checkInTime = processCheckIn(qrTicket);

    return CheckInResponseDto.builder()
        .message("체크인 완료되었습니다.")
        .checkInTime(checkInTime)
        .build();
  }

  // 회원 수동 코드 체크인
  public CheckInResponseDto checkIn(MemberManualCheckInRequestDto dto) {
    // 예약자 조회
    Attendee attendee = findAttendeeByReservation(dto.getReservationId());
    // QR 티켓 조회
    QrTicket qrTicket = findQrTicket(attendee);
    // 수동 코드 비교
    verifyManualCode(qrTicket, dto.getManualCode());
    // 재입장 여부 검토
    verifyReEntry(qrTicket);
    // QR 티켓 처리
    LocalDateTime checkInTime = processCheckIn(qrTicket);
    return CheckInResponseDto.builder()
        .message("체크인 완료되었습니다.")
        .checkInTime(checkInTime)
        .build();
  }

  // 비회원 QR 코드 체크인
  public CheckInResponseDto checkIn(GuestQrCheckInRequestDto dto) {
    // qr 티켓 링크 token 조회
    QrTicketRequestDto qrLinkTokenInfo = codeValidator.decodeToDto(dto.getQrLinkToken());
    // 참석자 조회
    Attendee attendee = findAttendeeByAttendee(qrLinkTokenInfo.getAttendeeId());
    // qr 티켓 조회
    QrTicket qrTicket = findQrTicket(attendee);
    // qrcode 비교
    verifyQrCode(qrTicket, dto.getQrCode());
    // 재입장 여부 검토
    verifyReEntry(qrTicket);
    // QR 티켓 처리
    LocalDateTime checkInTime = processCheckIn(qrTicket);
    return CheckInResponseDto.builder()
        .message("체크인 완료되었습니다.")
        .checkInTime(checkInTime)
        .build();
  }

  // 비회원 수동 코드 체크인
  public CheckInResponseDto checkIn(GuestManualCheckInRequestDto dto) {
    // qr 티켓 링크 token 조회
    QrTicketRequestDto qrLinkTokenInfo = codeValidator.decodeToDto(dto.getQrLinkToken());
    // 참석자 조회
    Attendee attendee = findAttendeeByAttendee(qrLinkTokenInfo.getAttendeeId());
    // qr 티켓 조회
    QrTicket qrTicket = findQrTicket(attendee);
    // 수동 코드 비교
    verifyManualCode(qrTicket, dto.getManualCode());
    // 재입장 여부 검토
    verifyReEntry(qrTicket);
    // QR 티켓 처리
    LocalDateTime checkInTime = processCheckIn(qrTicket);
    return CheckInResponseDto.builder()
        .message("체크인 완료되었습니다.")
        .checkInTime(checkInTime)
        .build();
  }

  // 회원 attendee 조회
  private Attendee findAttendeeByReservation(Long reservationId) {
    return attendeeRepository.findByReservation_ReservationIdAndAttendeeTypeCode_Code(
            reservationId, AttendeeTypeCode.PRIMARY)
        .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "참석자를 조회하지 못했습니다."));
  }

  // 비회원 attendee 조회
  private Attendee findAttendeeByAttendee(Long attendeeId) {
    return attendeeRepository.findById(attendeeId)
        .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "참석자를 조회하지 못했습니다."));
  }

  // QR 티켓 조회
  private QrTicket findQrTicket(Attendee attendee) {
    return qrTicketRepository.findByAttendee(attendee)
        .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "예약된 QR 티켓을 찾지 못했습니다."));
  }

  // QR 코드 검증
  private void verifyQrCode(QrTicket qrTicket, String qrCode) {
    if (!qrTicket.getActive()) {
      throw new CustomException(HttpStatus.BAD_REQUEST, "만료된 QR 티켓입니다.");
    }

    QrLog qrLog = qrLogRepository.findQrActionCode_Code("").orElse(null);

    // 재입장 여부 확인
    if (qrLog != null && !qrTicket.getReentryAllowed()) {
      throw new CustomException(HttpStatus.UNAUTHORIZED, "이미 입장한 이력이 있으며 재입장할 수 없습니다.");
    }

    if (!qrTicket.getQrCode().equals(qrCode)) {
      throw new CustomException(HttpStatus.UNAUTHORIZED, "참석자 정보와 일치하지 않습니다.");
    }
  }

  // 수동 코드 검증
  private void verifyManualCode(QrTicket qrTicket, String manualCode) {
    codeValidator.validateManualCode(manualCode);

    if (!qrTicket.getManualCode().equals(manualCode)) {
      throw new CustomException(HttpStatus.UNAUTHORIZED, "참석자 정보와 일치하지 않습니다.");
    }
  }

  // 재입장 가능 여부 검토
  private void verifyReEntry(QrTicket qrTicket) {
    // 체크인/체크아웃 로그 조회 (해당 티켓, 사용자 기준)
    Optional<QrCheckLog> qrLogCheckInOpt = qrCheckLogRepository.findTop1ByQrTicketAndCheckStatusCode_CodeOrderByCreatedAtDesc(
        qrTicket, "CHECKIN");
    Optional<QrCheckLog> qrLogCheckOutOpt = qrCheckLogRepository.findTop1ByQrTicketAndCheckStatusCode_CodeOrderByCreatedAtDesc(
        qrTicket, "CHECKOUT");

    // 체크아웃
    boolean checkOutAllowed = qrTicket.getEventTicket().getEvent().getEventDetail()
        .getCheckOutAllowed();

    boolean hasCheckIn = qrLogCheckInOpt.isPresent();
    boolean hasCheckOut = qrLogCheckOutOpt.isPresent();

    // 재입장 불가 + 입장 기록 있음
    if (!qrTicket.getReentryAllowed() && hasCheckIn) {
      throw new CustomException(HttpStatus.UNAUTHORIZED, "입장한 기록이 있으므로 재입장할 수 없습니다.");
    }

    // 재입장 불가 + 퇴장 기록 없음(퇴장 가능 이벤트에서)
    if (!qrTicket.getReentryAllowed() && checkOutAllowed && !hasCheckOut) {
      throw new CustomException(HttpStatus.UNAUTHORIZED, "퇴장한 기록이 없으므로 재입장할 수 없습니다.");
    }
  }

  // 검증 완료 후 디비 데이터 저장
  private LocalDateTime processCheckIn(QrTicket ticket) {
    // qr코드 상태변경 log
    qrLogSave(ticket, "CHECKIN");
    // 체크인 상태변경 log
    // 체크인 여부 확인
    LocalDateTime now = LocalDateTime.now();
    return now;
  }

  // QR 로그 저장
  private void qrLogSave(QrTicket qrTicket, String type){
    qrLogService.save(qrTicket, type);
  }
}
