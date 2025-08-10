package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.attendee.entity.AttendeeTypeCode;
import com.fairing.fairplay.attendee.repository.AttendeeRepository;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.qr.dto.CheckInRequestDto;
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

/*
 * 1. 재입장 가능 여부 검토
 * 2. 최초 입장인지 재입장인지 판단
 * 3. 코드 스캔 로그 기록
 * 4. QR 코드 일치 하는 지 검토
 * 5. QR 티켓 처리
 * - 중복 스캔 여부 검토 -> 로그 기록 후 예외
 * - 잘못된 스캔 여부 검토 -> 로그 기록 후 예외
 * - QR 체크인 로그 기록
 * */
@Service
@RequiredArgsConstructor
public class QrTicketEntryService {

  private final QrTicketRepository qrTicketRepository;
  private final AttendeeRepository attendeeRepository;
  private final CodeValidator codeValidator; // 검증 로직 분리
  private final QrLogService qrLogService;
  private final QrEntryValidateService qrEntryValidateService;

  private static final String QR = "QR";
  private static final String MANUAL = "MANUAL";

  // 회원 QR 코드 체크인
  public CheckInResponseDto checkIn(MemberQrCheckInRequestDto dto, CustomUserDetails userDetails) {
    // 예약자 조회
    Attendee attendee = findAttendee(dto.getReservationId(), null);
    CheckInRequestDto checkInRequestDto = CheckInRequestDto.builder()
        .attendee(attendee)
        .requireUserMatch(true)
        .userDetails(userDetails)
        .codeType(QR)
        .codeValue(dto.getQrCode())
        .qrActionCode(QrActionCode.CHECKED_IN)
        .build();

    return processCheckInCommon(checkInRequestDto);
  }

  // 회원 수동 코드 체크인
  public CheckInResponseDto checkIn(MemberManualCheckInRequestDto dto,
      CustomUserDetails userDetails) {
    // 예약자 조회
    Attendee attendee = findAttendee(dto.getReservationId(), null);
    CheckInRequestDto checkInRequestDto = CheckInRequestDto.builder()
        .attendee(attendee)
        .requireUserMatch(true)
        .userDetails(userDetails)
        .codeType(MANUAL)
        .codeValue(dto.getManualCode())
        .qrActionCode(QrActionCode.MANUAL_CHECKED_IN)
        .build();

    return processCheckInCommon(checkInRequestDto);
  }

  // 비회원 QR 코드 체크인
  public CheckInResponseDto checkIn(GuestQrCheckInRequestDto dto) {
    // qr 티켓 링크 token 조회
    QrTicketRequestDto qrLinkTokenInfo = codeValidator.decodeToDto(dto.getQrLinkToken());

    // 예약자 조회
    Attendee attendee = findAttendee(null, qrLinkTokenInfo.getAttendeeId());
    CheckInRequestDto checkInRequestDto = CheckInRequestDto.builder()
        .attendee(attendee)
        .requireUserMatch(false)
        .userDetails(null)
        .codeType(QR)
        .codeValue(dto.getQrCode())
        .qrActionCode(QrActionCode.CHECKED_IN)
        .build();

    return processCheckInCommon(checkInRequestDto);
  }

  // 비회원 수동 코드 체크인
  public CheckInResponseDto checkIn(GuestManualCheckInRequestDto dto) {
    // qr 티켓 링크 token 조회
    QrTicketRequestDto qrLinkTokenInfo = codeValidator.decodeToDto(dto.getQrLinkToken());

    // 예약자 조회
    Attendee attendee = findAttendee(null, qrLinkTokenInfo.getAttendeeId());
    CheckInRequestDto checkInRequestDto = CheckInRequestDto.builder()
        .attendee(attendee)
        .requireUserMatch(false)
        .userDetails(null)
        .codeType(MANUAL)
        .codeValue(dto.getManualCode())
        .qrActionCode(QrActionCode.MANUAL_CHECKED_IN)
        .build();

    return processCheckInCommon(checkInRequestDto);
  }

  /**
   * Attendee 조회 (회원은 reservationId, 비회원은 attendeeId로)
   */
  private Attendee findAttendee(Long reservationId, Long attendeeId) {
    if (reservationId != null) {
      return findAttendeeByReservation(reservationId);
    }
    if (attendeeId != null) {
      return findAttendeeByAttendee(attendeeId);
    }
    throw new CustomException(HttpStatus.BAD_REQUEST, "reservationId 또는 attendeeId 중 하나는 필수입니다.");
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

  /**
   * 로그인 사용자와 참석자 일치 여부 확인
   */
  private void validateUserMatch(Attendee attendee, CustomUserDetails userDetails) {
    if (!attendee.getReservation().getUser().getUserId().equals(userDetails.getUserId())) {
      throw new CustomException(HttpStatus.FORBIDDEN, "참석자와 로그인한 사용자가 일치하지 않습니다.");
    }
  }

  /**
   * QR 티켓 조회
   */
  private QrTicket findQrTicket(Attendee attendee) {
    return qrTicketRepository.findByAttendee(attendee)
        .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "예약된 QR 티켓을 찾지 못했습니다."));
  }

  /**
   * 코드 검증
   */
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

  /**
   * 체크인 공통 로직
   */
  private CheckInResponseDto processCheckInCommon(CheckInRequestDto dto) {
    if (dto.isRequireUserMatch()) {
      // 회원, 비회원 여부에 따라 참석자=로그인한 사용자 여부 조회
      validateUserMatch(dto.getAttendee(), dto.getUserDetails());
    }
    // QR 티켓 조회
    QrTicket qrTicket = findQrTicket(dto.getAttendee());
    // QrActionCode 검토
    QrActionCode qrActionCode = qrEntryValidateService.validateQrActionCode(QrActionCode.SCANNED);
    // 중복 스캔 -> QrLog: invalid, QrChecktLog: duplicate 저장
    qrEntryValidateService.preventDuplicateScan(qrTicket, qrActionCode.getCode());
    // 코드 스캔 기록
    qrLogService.scannedQrLog(qrTicket, qrActionCode);
    // 재입장 가능 여부 검토 - 입장 기록 자체가 있는지 조회
    qrEntryValidateService.verifyReEntry(qrTicket);
    // 현재 입장이 최초입장인지 재입장인지 판단 QrCheckStatusCode.ENTRY, rCheckStatusCode.REENTRY
    String entryType = qrLogService.determineEntryOrReEntry(qrTicket);
    // 코드 비교
    if (QR.equals(dto.getCodeType())) {
      verifyQrCode(qrTicket, dto.getCodeValue());
    } else {
      verifyManualCode(qrTicket, dto.getCodeValue());
    }
    // QR 티켓 처리
    LocalDateTime checkInTime = processCheckIn(qrTicket, dto.getQrActionCode(), entryType);
    return CheckInResponseDto.builder()
        .message("체크인 완료되었습니다.")
        .checkInTime(checkInTime)
        .build();
  }

  // 검증 완료 후 디비 데이터 저장
  private LocalDateTime processCheckIn(QrTicket qrTicket, String checkInType, String entryType) {
    // actionCodeType = CHECKED_IN, MANUAL_CHECKED_IN
    QrActionCode qrActionCode = qrEntryValidateService.validateQrActionCode(checkInType);
    // checkStatusCode = ENTRY, REENTRY
    QrCheckStatusCode qrCheckStatusCode = qrEntryValidateService.validateQrCheckStatusCode(
        entryType);
    // 잘못된 입퇴장 스캔 -> ENTRY, REENTRY
    qrEntryValidateService.preventInvalidScan(qrTicket, qrCheckStatusCode.getCode());
    return qrLogService.entryQrLog(qrTicket, qrActionCode, qrCheckStatusCode);
  }
}
