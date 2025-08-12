package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.attendee.entity.AttendeeTypeCode;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.qr.dto.scan.CheckOutRequestDto;
import com.fairing.fairplay.qr.dto.scan.CheckResponseDto;
import com.fairing.fairplay.qr.dto.scan.ManualCheckRequestDto;
import com.fairing.fairplay.qr.dto.scan.QrCheckRequestDto;
import com.fairing.fairplay.qr.dto.scan.QrCodeDecodeDto;
import com.fairing.fairplay.qr.entity.QrActionCode;
import com.fairing.fairplay.qr.entity.QrCheckStatusCode;
import com.fairing.fairplay.qr.entity.QrTicket;
import com.fairing.fairplay.qr.repository.QrTicketRepository;
import com.fairing.fairplay.qr.util.CodeValidator;
import com.fairing.fairplay.user.entity.Users;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QrTicketExitService {

  private final QrTicketRepository qrTicketRepository;
  private final QrTicketVerificationService qrTicketVerificationService;
  private final QrLogService qrLogService;
  private final QrTicketAttendeeService qrTicketAttendeeService;
  private final QrEntryValidateService qrEntryValidateService;
  private final CodeValidator codeValidator;

  private static final String QR = "QR";
  private static final String MANUAL = "MANUAL";

  // 회원 QR 코드 체크아웃
  public CheckResponseDto checkOutWithQr(QrCheckRequestDto dto) {
    // 코드 디코딩
    QrCodeDecodeDto qrCodeDecodeDto = codeValidator.decodeToQrTicket(dto.getQrCode());

    // QR 코드 토큰 이용한 티켓 조회
    QrTicket qrTicket = qrTicketRepository.findById(qrCodeDecodeDto.getQrTicketId()).orElseThrow(
        () -> new CustomException(HttpStatus.NOT_FOUND, "올바르지 않은 QR 코드입니다.")
    );

    // QR 티켓 참석자 조회
    Attendee attendee = qrTicket.getAttendee();
    AttendeeTypeCode primaryTypeCode = qrTicketAttendeeService.findPrimaryTypeCode();
    AttendeeTypeCode guestTypeCode = qrTicketAttendeeService.findGuestTypeCode();
    // 회원인지 검증
    if (attendee.getAttendeeTypeCode().equals(primaryTypeCode)) {
      // 회원인지 검증
      Users user = qrTicket.getAttendee().getReservation().getUser();
      qrTicketVerificationService.validateUser(user);
    } else if (attendee.getAttendeeTypeCode().equals(guestTypeCode)) {
      // 비회원일 경우 qr 티켓의 참석자와 qrcode에 저장된 참석자 정보가 일치하는지 판단
      if (qrCodeDecodeDto.getAttendeeId() == null) {
        throw new CustomException(HttpStatus.BAD_REQUEST, "QR 코드에 참석자 정보가 없습니다.");
      }
      if (!attendee.getId().equals(qrCodeDecodeDto.getAttendeeId())) {
        throw new CustomException(HttpStatus.NOT_FOUND, "참석자와 일치하는 QR 티켓이 없습니다.");
      }
    } else {
      throw new CustomException(HttpStatus.BAD_REQUEST, "지원하지 않는 참석자 유형입니다.");
    }

    CheckOutRequestDto checkOutRequestDto = CheckOutRequestDto.builder()
        .attendee(attendee)
        .codeType(QR)
        .codeValue(dto.getQrCode())
        .build();

    return processCheckOutCommon(qrTicket, checkOutRequestDto);
  }

  // 회원 수동 코드 체크아웃
  public CheckResponseDto checkOutWithManual(ManualCheckRequestDto dto) {
    QrTicket qrTicket = qrTicketRepository.findByManualCode(dto.getManualCode()).orElseThrow(
        () -> new CustomException(HttpStatus.NOT_FOUND, "올바르지 않은 수동 코드입니다.")
    );

    // QR 티켓에 저장된 참석자 조회
    Attendee attendee = qrTicket.getAttendee();
    AttendeeTypeCode primaryTypeCode = qrTicketAttendeeService.findPrimaryTypeCode();
    // 회원인지 검증
    if (attendee.getAttendeeTypeCode().equals(primaryTypeCode)) {
      Users user = qrTicket.getAttendee().getReservation().getUser();
      qrTicketVerificationService.validateUser(user);
    }

    CheckOutRequestDto checkOutRequestDto = CheckOutRequestDto.builder()
        .attendee(attendee)
        .codeType(MANUAL)
        .codeValue(dto.getManualCode())
        .build();

    return processCheckOutCommon(qrTicket, checkOutRequestDto);
  }

  /**
   * 체크아웃 공통 로직
   */
  private CheckResponseDto processCheckOutCommon(QrTicket qrTicket, CheckOutRequestDto dto) {
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
