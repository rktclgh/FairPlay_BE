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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class QrTicketExitService {

  private final QrTicketRepository qrTicketRepository;
  private final QrTicketVerificationService qrTicketVerificationService;
  private final QrLogService qrLogService;
  private final QrTicketAttendeeService qrTicketAttendeeService;
  private final QrEntryValidateService qrEntryValidateService;
  private final CodeValidator codeValidator;
  private final SimpMessagingTemplate messagingTemplate;

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
    // 코드 스캔 기록
    qrLogService.scannedQrLog(qrTicket, qrActionCode);
    log.info("qrActionCode : {}", qrActionCode);
    log.info("QrLog SCANNED 저장 qrTicket: {}",qrTicket.getId());
    // 체크아웃이 가능한가 -> 규칙상 (체크아웃 스캔 자체가 가능한지 판단)
    qrEntryValidateService.checkEntryExitPolicy(qrTicket, QrCheckStatusCode.EXIT);
    QrCheckStatusCode qrCheckStatusCode = qrEntryValidateService.validateQrCheckStatusCode(
        QrCheckStatusCode.EXIT);
    // 재입장 가능 여부 검토
    qrEntryValidateService.verifyCheckOutReEntry(qrTicket);
    // 중복 스캔 -> QrLog: invalid, QrChecktLog: duplicate 저장
    qrEntryValidateService.preventDuplicateScan(qrTicket, qrCheckStatusCode.getCode());
    // 코드 비교
    if (QR.equals(dto.getCodeType())) {
      log.info("체크아웃 타입:QR");
      qrTicketVerificationService.verifyQrCode(qrTicket, dto.getCodeValue());
    } else {
      log.info("체크아웃 타입:MANUAL");
      qrTicketVerificationService.verifyManualCode(qrTicket, dto.getCodeValue());
    }
    // QR 티켓 처리
    LocalDateTime checkInTime = processCheckOut(qrTicket, qrCheckStatusCode.getCode());
    checkOutQrTicket(qrTicket);
    return CheckResponseDto.builder()
        .message("체크아웃 완료되었습니다.")
        .checkInTime(checkInTime)
        .build();
  }

  // 검증 완료 후 디비 데이터 저장
  private LocalDateTime processCheckOut(QrTicket qrTicket, String entryType) {
    log.info("qrTicket : {} DB 저장 시작",qrTicket.getId());
    // checkStatusCode = EXIT
    QrCheckStatusCode qrCheckStatusCode = qrEntryValidateService.validateQrCheckStatusCode(
        entryType);
    // 잘못된 입퇴장 스캔 -> EXIT
    qrEntryValidateService.preventInvalidScan(qrTicket, qrCheckStatusCode.getCode());
    return qrLogService.exitQrLog(qrTicket, qrCheckStatusCode);
  }
  public void checkOutQrTicket(QrTicket qrTicket){
    messagingTemplate.convertAndSend("/topic/check-out/"+qrTicket.getId(),"체크아웃 처리가 완료되었습니다.");
  }
}
