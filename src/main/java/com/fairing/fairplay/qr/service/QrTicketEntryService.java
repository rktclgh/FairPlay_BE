package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.attendee.entity.AttendeeTypeCode;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.qr.dto.scan.AdminCheckRequestDto;
import com.fairing.fairplay.qr.dto.scan.AdminForceCheckRequestDto;
import com.fairing.fairplay.qr.dto.scan.CheckInRequestDto;
import com.fairing.fairplay.qr.dto.scan.CheckResponseDto;
import com.fairing.fairplay.qr.dto.scan.ManualCheckRequestDto;
import com.fairing.fairplay.qr.dto.scan.QrCheckRequestDto;
import com.fairing.fairplay.qr.entity.QrActionCode;
import com.fairing.fairplay.qr.entity.QrCheckStatusCode;
import com.fairing.fairplay.qr.entity.QrTicket;
import com.fairing.fairplay.qr.repository.QrTicketRepository;
import com.fairing.fairplay.user.entity.Users;
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
  private final QrTicketVerificationService qrTicketVerificationService;
  private final QrLogService qrLogService;
  private final QrTicketAttendeeService qrTicketAttendeeService;
  private final QrEntryValidateService qrEntryValidateService;

  private static final String QR = "QR";
  private static final String MANUAL = "MANUAL";

  // QR 체크인
  public CheckResponseDto checkInWithQr(QrCheckRequestDto dto) {
    QrTicket qrTicket = qrTicketRepository.findByQrCode(dto.getQrCode()).orElseThrow(
        () -> new CustomException(HttpStatus.NOT_FOUND, "올바르지 않은 QR 코드입니다.")
    );

    // QR 티켓 참석자 조회
    Attendee attendee = qrTicket.getAttendee();

    CheckInRequestDto checkInRequestDto = CheckInRequestDto.builder()
        .attendee(attendee)
        .codeType(QR)
        .codeValue(qrTicket.getQrCode())
        .qrActionCode(QrActionCode.CHECKED_IN)
        .build();

    return processCheckInCommon(qrTicket, checkInRequestDto);
  }

  // 수동 코드 체크인
  public CheckResponseDto checkInWithManual(ManualCheckRequestDto dto) {
    QrTicket qrTicket = qrTicketRepository.findByManualCode(dto.getManualCode()).orElseThrow(
        () -> new CustomException(HttpStatus.NOT_FOUND, "올바르지 않은 수동 코드입니다.")
    );

    Attendee attendee = qrTicket.getAttendee();
    CheckInRequestDto checkInRequestDto = CheckInRequestDto.builder()
        .attendee(attendee)
        .codeType(MANUAL)
        .codeValue(qrTicket.getQrCode())
        .qrActionCode(QrActionCode.MANUAL_CHECKED_IN)
        .build();
    return processCheckInCommon(qrTicket, checkInRequestDto);
  }

  // 강제 입퇴장 체크인
  public CheckResponseDto adminForceCheck(AdminForceCheckRequestDto dto) {
    return processAdminForceCheck(dto);
  }

  /**
   * 체크인 공통 로직
   */
  private CheckResponseDto processCheckInCommon(QrTicket qrTicket, CheckInRequestDto dto) {
    Attendee attendee = dto.getAttendee();
    AttendeeTypeCode primaryTypeCode = qrTicketAttendeeService.findPrimaryTypeCode();
    // 회원이 대표자일 경우 회원 검증 진행
    if (attendee.getAttendeeTypeCode().equals(primaryTypeCode)) {
      dto.setRequireUserMatch(Boolean.TRUE);
    }
    // 회원인지 검증
    if (dto.isRequireUserMatch()) {
      Users user = qrTicket.getAttendee().getReservation().getUser();
      qrTicketVerificationService.validateUser(user);
    }
    // QrActionCode 검토
    QrActionCode qrActionCode = qrEntryValidateService.validateQrActionCode(QrActionCode.SCANNED);
    // 코드 스캔 기록
    qrLogService.scannedQrLog(qrTicket, qrActionCode);
    // 재입장 가능 여부 검토 - 입장 기록 자체가 있는지 조회
    qrEntryValidateService.verifyReEntry(qrTicket);
    // 현재 입장이 최초입장인지 재입장인지 판단 QrCheckStatusCode.ENTRY, rCheckStatusCode.REENTRY
    String entryType = qrLogService.determineEntryOrReEntry(qrTicket);
    // 중복 스캔 -> QrLog: invalid, QrChecktLog: duplicate 저장
    qrEntryValidateService.preventDuplicateScan(qrTicket, entryType);
    // 코드 비교
    if (QR.equals(dto.getCodeType())) {
      qrTicketVerificationService.verifyQrCode(qrTicket, dto.getCodeValue());
    } else {
      qrTicketVerificationService.verifyManualCode(qrTicket, dto.getCodeValue());
    }
    // QR 티켓 처리
    LocalDateTime checkInTime = processCheckIn(qrTicket, dto.getQrActionCode(), entryType);
    return CheckResponseDto.builder()
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

  // 관리자 강제 입퇴장
  private CheckResponseDto processAdminForceCheck(AdminForceCheckRequestDto dto) {
    QrTicket qrTicket = qrTicketRepository.findByTicketNo(dto.getTicketNo()).orElseThrow(
        () -> new CustomException(HttpStatus.NOT_FOUND, "티켓 번호와 일치하는 티켓이 없습니다.")
    );

    Attendee attendee = qrTicket.getAttendee();

    QrCheckStatusCode qrCheckStatusCode = qrEntryValidateService.validateQrCheckStatusCode(
        dto.getQrCheckStatusCode());
    QrActionCode qrActionCode = switch (qrCheckStatusCode.getCode()) {
      case QrCheckStatusCode.ENTRY ->
          qrEntryValidateService.validateQrActionCode(QrActionCode.FORCE_CHECKED_IN);
      case QrCheckStatusCode.EXIT ->
          qrEntryValidateService.validateQrActionCode(QrActionCode.FORCE_CHECKED_OUT);
      default -> throw new CustomException(HttpStatus.BAD_REQUEST, "유효하지 않은 체크 상태 코드입니다.");
    };

    AdminCheckRequestDto adminCheckRequestDto = AdminCheckRequestDto.builder()
        .attendee(attendee)
        .qrTicket(qrTicket)
        .qrActionCode(qrActionCode)
        .qrCheckStatusCode(qrCheckStatusCode)
        .build();
    // 강제 처리 → 검증 로직 건너뛰고 바로 기록
    LocalDateTime checkInTime = qrLogService.forceCheckQrLog(adminCheckRequestDto.getQrTicket(),
        adminCheckRequestDto.getQrActionCode(),
        adminCheckRequestDto.getQrCheckStatusCode());
    return CheckResponseDto.builder()
        .message("관리자 강제 " + adminCheckRequestDto.getQrCheckStatusCode().getCode() + " 완료")
        .checkInTime(checkInTime)
        .build();
  }
}
