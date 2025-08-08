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
import com.fairing.fairplay.qr.entity.QrTicket;
import com.fairing.fairplay.qr.repository.QrTicketRepository;
import com.fairing.fairplay.qr.util.CodeValidator;
import java.time.LocalDateTime;
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

  // 회원 QR 코드 체크인
  public CheckInResponseDto checkIn(MemberQrCheckInRequestDto dto) {
    // 예약자 조회
    Attendee attendee = findAttendeeByReservation(dto.getReservationId());
    // QR 티켓 조회
    QrTicket qrTicket = findQrTicket(attendee);
    // qrcode 비교
    verifyQrCode(qrTicket, dto.getQrCode());
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

    // 재입장 여부 확인

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

  // 검증 완료 후 디비 데이터 저장
  private LocalDateTime processCheckIn(QrTicket ticket) {
    // qr코드 상태변경 log
    // 체크인 상태변경 log
    // 체크인 여부 확인
    LocalDateTime now = LocalDateTime.now();
    return now;
  }


}
