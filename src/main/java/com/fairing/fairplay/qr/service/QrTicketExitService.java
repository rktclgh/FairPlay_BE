package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.qr.dto.QrTicketRequestDto;
import com.fairing.fairplay.qr.dto.scan.CheckOutRequestDto;
import com.fairing.fairplay.qr.dto.scan.CheckResponseDto;
import com.fairing.fairplay.qr.dto.scan.GuestManualCheckRequestDto;
import com.fairing.fairplay.qr.dto.scan.GuestQrCheckRequestDto;
import com.fairing.fairplay.qr.dto.scan.MemberManualCheckRequestDto;
import com.fairing.fairplay.qr.dto.scan.MemberQrCheckRequestDto;
import com.fairing.fairplay.qr.entity.QrActionCode;
import com.fairing.fairplay.qr.entity.QrCheckStatusCode;
import com.fairing.fairplay.qr.entity.QrTicket;
import com.fairing.fairplay.qr.util.CodeValidator;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QrTicketExitService {

  private final CodeValidator codeValidator; // 검증 로직 분리
  private final QrTicketVerificationService qrTicketVerificationService;
  private final QrLogService qrLogService;
  private final QrTicketAttendeeService qrTicketAttendeeService;
  private final QrEntryValidateService qrEntryValidateService;

  private static final String QR = "QR";
  private static final String MANUAL = "MANUAL";

  // 회원 QR 코드 체크아웃
  public CheckResponseDto checkOut(MemberQrCheckRequestDto dto,
      CustomUserDetails userDetails) {
    // 예약자 조회
    Attendee attendee = qrTicketAttendeeService.findAttendee(dto.getReservationId(), null);
    CheckOutRequestDto checkOutRequestDto = CheckOutRequestDto.builder()
        .attendee(attendee)
        .requireUserMatch(true)
        .userDetails(userDetails)
        .codeType(QR)
        .codeValue(dto.getQrCode())
        .build();

    return processCheckOutCommon(checkOutRequestDto);
  }

  // 회원 수동 코드 체크아웃
  public CheckResponseDto checkOut(MemberManualCheckRequestDto dto,
      CustomUserDetails userDetails) {
    // 예약자 조회
    Attendee attendee = qrTicketAttendeeService.findAttendee(dto.getReservationId(), null);
    CheckOutRequestDto checkOutRequestDto = CheckOutRequestDto.builder()
        .attendee(attendee)
        .requireUserMatch(true)
        .userDetails(userDetails)
        .codeType(MANUAL)
        .codeValue(dto.getManualCode())
        .build();

    return processCheckOutCommon(checkOutRequestDto);
  }

  // 비회원 QR 코드 체크아웃
  public CheckResponseDto checkOut(GuestQrCheckRequestDto dto) {
    // qr 티켓 링크 token 조회
    QrTicketRequestDto qrLinkTokenInfo = codeValidator.decodeToDto(dto.getQrLinkToken());

    // 예약자 조회
    Attendee attendee = qrTicketAttendeeService.findAttendee(null, qrLinkTokenInfo.getAttendeeId());
    CheckOutRequestDto checkOutRequestDto = CheckOutRequestDto.builder()
        .attendee(attendee)
        .requireUserMatch(false)
        .userDetails(null)
        .codeType(QR)
        .codeValue(dto.getQrCode())
        .build();

    return processCheckOutCommon(checkOutRequestDto);
  }

  // 비회원 수동 코드 체크아웃
  public CheckResponseDto checkOut(GuestManualCheckRequestDto dto) {
    // qr 티켓 링크 token 조회
    QrTicketRequestDto qrLinkTokenInfo = codeValidator.decodeToDto(dto.getQrLinkToken());

    // 예약자 조회
    Attendee attendee = qrTicketAttendeeService.findAttendee(null, qrLinkTokenInfo.getAttendeeId());
    CheckOutRequestDto checkOutRequestDto = CheckOutRequestDto.builder()
        .attendee(attendee)
        .requireUserMatch(false)
        .userDetails(null)
        .codeType(MANUAL)
        .codeValue(dto.getManualCode())
        .build();

    return processCheckOutCommon(checkOutRequestDto);
  }

  /**
   * 체크아웃 공통 로직
   */
  private CheckResponseDto processCheckOutCommon(CheckOutRequestDto dto) {
    if (dto.isRequireUserMatch()) {
      // 회원, 비회원 여부에 따라 참석자=로그인한 사용자 여부 조회
      qrTicketVerificationService.validateUserMatch(dto.getAttendee(), dto.getUserDetails());
    }
    // QR 티켓 조회
    QrTicket qrTicket = qrTicketVerificationService.findQrTicket(dto.getAttendee());
    // QrActionCode 검토
    QrActionCode qrActionCode = qrEntryValidateService.validateQrActionCode(QrActionCode.SCANNED);
    QrCheckStatusCode qrCheckStatusCode = qrEntryValidateService.validateQrCheckStatusCode(
        QrCheckStatusCode.EXIT);
    // 코드 스캔 기록
    qrLogService.scannedQrLog(qrTicket, qrActionCode);
    // 재입장 가능 여부 검토 - 퇴장 기록 자체가 있는지 조회
    qrEntryValidateService.verifyReEntry(qrTicket);
    // 중복 스캔 -> QrLog: invalid, QrChecktLog: duplicate 저장
    qrEntryValidateService.preventDuplicateScan(qrTicket, qrCheckStatusCode.getCode());
    // 코드 비교
    if (QR.equals(dto.getCodeType())) {
      qrTicketVerificationService.verifyQrCode(qrTicket, dto.getCodeValue());
    } else {
      qrTicketVerificationService.verifyManualCode(qrTicket, dto.getCodeValue());
    }
    // QR 티켓 처리
    LocalDateTime checkInTime = processCheckOut(qrTicket, qrCheckStatusCode.getCode());
    return CheckResponseDto.builder()
        .message("체크아웃 완료되었습니다.")
        .checkInTime(checkInTime)
        .build();
  }

  // 검증 완료 후 디비 데이터 저장
  private LocalDateTime processCheckOut(QrTicket qrTicket, String entryType) {
    // checkStatusCode = EXIT
    QrCheckStatusCode qrCheckStatusCode = qrEntryValidateService.validateQrCheckStatusCode(
        entryType);
    // 잘못된 입퇴장 스캔 -> EXIT
    qrEntryValidateService.preventInvalidScan(qrTicket, qrCheckStatusCode.getCode());
    return qrLogService.exitQrLog(qrTicket, qrCheckStatusCode);
  }
}
