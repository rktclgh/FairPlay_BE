package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.attendee.entity.AttendeeTypeCode;
import com.fairing.fairplay.attendee.repository.AttendeeRepository;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.qr.dto.CheckInResponseDto;
import com.fairing.fairplay.qr.dto.GuestManualCheckInRequestDto;
import com.fairing.fairplay.qr.dto.GuestQrCheckInRequestDto;
import com.fairing.fairplay.qr.dto.MemberManualCheckInRequestDto;
import com.fairing.fairplay.qr.dto.MemberQrCheckInRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketRequestDto;
import com.fairing.fairplay.qr.entity.QrActionCode;
import com.fairing.fairplay.qr.entity.QrCheckStatusCode;
import com.fairing.fairplay.qr.entity.QrTicket;
import com.fairing.fairplay.qr.repository.QrTicketRepository;
import com.fairing.fairplay.qr.util.CodeValidator;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QrTicketEntryService {

  private final QrTicketRepository qrTicketRepository;
  private final AttendeeRepository attendeeRepository;
  private final CodeValidator codeValidator; // 검증 로직 분리
  private final QrLogService qrLogService;
  private final QrEntryValidateService qrEntryValidateService;

  // 회원 QR 코드 체크인
  public CheckInResponseDto checkIn(MemberQrCheckInRequestDto dto, CustomUserDetails userDetails) {
    // 예약자 조회
    Attendee attendee = findAttendeeByReservation(dto.getReservationId());
    // 예약자와 현재 로그인한 사용자 일치 여부 조회
    if (!userDetails.getUserId().equals(attendee.getReservation().getUser().getUserId())) {
      throw new CustomException(HttpStatus.UNAUTHORIZED, "예약자와 현재 로그인한 사용자가 일치하지 않습니다.");
    }
    // QR 티켓 조회
    QrTicket qrTicket = findQrTicket(attendee);
    // 코드 스캔 기록
    qrLogService.scannedQrLog(qrTicket, QrActionCode.SCANNED);
    /*
    * 1. 재입장 가능 여부 검토 => qrEntryPolicyService
    * 2. 최초 입장인지 재입장인지 판단 => qrLogService
    * 3. 코드 스캔 로그 기록 => qrLogService
    * 4. QR 코드 일치 하는 지 검토 => qrTicketEntryService
    * 5. QR 티켓 처리 => qrTicketEntryService
    * - 중복 스캔 여부 검토 -> 로그 기록 후 예외 => qrLogService
    * - 잘못된 스캔 여부 검토 -> 로그 기록 후 예외 => qrLogService
    * - QR 체크인 로그 기록 => qrLogService
    * */
    // 재입장 가능 여부 검토 - 입장 기록 자체가 있는지 조회
    qrEntryValidateService.verifyReEntry(qrTicket);
    // 현재 입장이 최초입장인지 재입장인지 판단 QrCheckStatusCode.ENTRY, rCheckStatusCode.REENTRY
    String entryType = qrLogService.determineEntryOrReEntry(qrTicket);
    // qrcode 비교
    verifyQrCode(qrTicket, dto.getQrCode());
    // QR 티켓 처리
    LocalDateTime checkInTime = processCheckIn(qrTicket, QrActionCode.CHECKED_IN, entryType);

    return CheckInResponseDto.builder()
        .message("체크인 완료되었습니다.")
        .checkInTime(checkInTime)
        .build();
  }

  // 회원 수동 코드 체크인
  public CheckInResponseDto checkIn(MemberManualCheckInRequestDto dto,
      CustomUserDetails userDetails) {
    // 예약자 조회
    Attendee attendee = findAttendeeByReservation(dto.getReservationId());
    // 예약자와 현재 로그인한 사용자 일치 여부 조회
    if (!userDetails.getUserId().equals(attendee.getReservation().getUser().getUserId())) {
      throw new CustomException(HttpStatus.UNAUTHORIZED, "예약자와 현재 로그인한 사용자가 일치하지 않습니다.");
    }
    // QR 티켓 조회
    QrTicket qrTicket = findQrTicket(attendee);
    // 재입장 여부 검토

    // 수동 코드 비교
    verifyManualCode(qrTicket, dto.getManualCode());
    // QR 티켓 처리
    LocalDateTime checkInTime = processCheckIn(qrTicket, QrActionCode.MANUAL_CHECKED_IN,
        true ? QrCheckStatusCode.REENTRY : QrCheckStatusCode.ENTRY);
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
    // 코드 스캔 기록
    qrLogService.scannedQrLog(qrTicket, QrActionCode.SCANNED);
    // 재입장 여부 검토

    // qrcode 비교
    verifyQrCode(qrTicket, dto.getQrCode());
    // QR 티켓 처리
    LocalDateTime checkInTime = processCheckIn(qrTicket, QrActionCode.CHECKED_IN,
        true ? QrCheckStatusCode.REENTRY : QrCheckStatusCode.ENTRY);
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
    // 재입장 여부 검토

    // 수동 코드 비교
    verifyManualCode(qrTicket, dto.getManualCode());
    // QR 티켓 처리
    LocalDateTime checkInTime = processCheckIn(qrTicket, QrActionCode.MANUAL_CHECKED_IN,
        true ? QrCheckStatusCode.REENTRY : QrCheckStatusCode.ENTRY);
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
  private LocalDateTime processCheckIn(QrTicket qrTicket, String checkInType, String entryType) {
    return qrLogService.entryQrLog(qrTicket, checkInType, entryType);
  }
}
