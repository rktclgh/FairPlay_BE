package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.attendee.entity.AttendeeTypeCode;
import com.fairing.fairplay.booth.dto.BoothEntryRequestDto;
import com.fairing.fairplay.booth.entity.BoothExperienceReservation;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.qr.dto.scan.AdminCheckRequestDto;
import com.fairing.fairplay.qr.dto.scan.AdminForceCheckRequestDto;
import com.fairing.fairplay.qr.dto.scan.CheckInRequestDto;
import com.fairing.fairplay.qr.dto.scan.CheckResponseDto;
import com.fairing.fairplay.qr.dto.scan.ManualCheckRequestDto;
import com.fairing.fairplay.qr.dto.scan.QrCheckRequestDto;
import com.fairing.fairplay.qr.dto.scan.QrCodeDecodeDto;
import com.fairing.fairplay.qr.entity.QrActionCode;
import com.fairing.fairplay.qr.entity.QrCheckStatusCode;
import com.fairing.fairplay.qr.entity.QrTicket;
import com.fairing.fairplay.qr.repository.QrActionCodeRepository;
import com.fairing.fairplay.qr.repository.QrTicketRepository;
import com.fairing.fairplay.qr.util.CodeValidator;
import com.fairing.fairplay.reservation.entity.Reservation;
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
public class QrTicketEntryService {

  private final QrTicketRepository qrTicketRepository;
  private final QrTicketVerificationService qrTicketVerificationService;
  private final QrLogService qrLogService;
  private final QrTicketAttendeeService qrTicketAttendeeService;
  private final QrEntryValidateService qrEntryValidateService;
  private final SimpMessagingTemplate messagingTemplate;

  private static final String QR = "QR";
  private static final String MANUAL = "MANUAL";
  private final CodeValidator codeValidator;
  private final QrActionCodeRepository qrActionCodeRepository;

  // QR 체크인
  public CheckResponseDto checkInWithQr(QrCheckRequestDto dto) {
    // 코드 디코딩
    QrCodeDecodeDto qrCodeDecodeDto = codeValidator.decodeToQrTicket(dto.getQrCode());

    // QR 코드 토큰 이용한 티켓 조회
    QrTicket qrTicket = qrTicketRepository.findById(qrCodeDecodeDto.getQrTicketId()).orElseThrow(
        () -> new CustomException(HttpStatus.NOT_FOUND, "올바르지 않은 QR 코드입니다.")
    );

    LocalDateTime now = LocalDateTime.now(); // 현재일시
    if(now.isAfter(qrTicket.getExpiredAt())){
      throw new CustomException(HttpStatus.BAD_REQUEST, "행사가 종료되었습니다.");
    }

    // QR 티켓 참석자 조회
    Attendee attendee = qrTicket.getAttendee();
    AttendeeTypeCode primaryTypeCode = qrTicketAttendeeService.findPrimaryTypeCode();
    AttendeeTypeCode guestTypeCode = qrTicketAttendeeService.findGuestTypeCode();
    // 회원인지 검증
    if (attendee.getAttendeeTypeCode().equals(primaryTypeCode)) {
      // 회원인지 검증
      Users user = qrTicket.getAttendee().getReservation().getUser();
      qrTicketVerificationService.validateUser(user);
    }else if(attendee.getAttendeeTypeCode().equals(guestTypeCode)) {
      // 비회원일 경우 qr 티켓의 참석자와 qrcode에 저장된 참석자 정보가 일치하는지 판단
      if (qrCodeDecodeDto.getAttendeeId() == null) {
        throw new CustomException(HttpStatus.BAD_REQUEST, "QR 코드에 참석자 정보가 없습니다.");
      }
      if(!attendee.getId().equals(qrCodeDecodeDto.getAttendeeId())){
        throw new CustomException(HttpStatus.NOT_FOUND,"참석자와 일치하는 QR 티켓이 없습니다.");
      }
    }else {
      throw new CustomException(HttpStatus.BAD_REQUEST, "지원하지 않는 참석자 유형입니다.");
    }

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

    LocalDateTime now = LocalDateTime.now(); // 현재일시
    if(now.isAfter(qrTicket.getExpiredAt())){
      throw new CustomException(HttpStatus.BAD_REQUEST, "행사가 종료되었습니다.");
    }

    // QR 티켓에 저장된 참석자 조회
    Attendee attendee = qrTicket.getAttendee();
    AttendeeTypeCode primaryTypeCode = qrTicketAttendeeService.findPrimaryTypeCode();
    // 회원인지 검증
    if (attendee.getAttendeeTypeCode().equals(primaryTypeCode)) {
      Users user = qrTicket.getAttendee().getReservation().getUser();
      qrTicketVerificationService.validateUser(user);
    }

    CheckInRequestDto checkInRequestDto = CheckInRequestDto.builder()
        .attendee(attendee)
        .codeType(MANUAL)
        .codeValue(qrTicket.getManualCode())
        .qrActionCode(QrActionCode.MANUAL_CHECKED_IN)
        .build();
    return processCheckInCommon(qrTicket, checkInRequestDto);
  }

  // 강제 입퇴장 체크인
  public CheckResponseDto adminForceCheck(AdminForceCheckRequestDto dto) {
    return processAdminForceCheck(dto);
  }

  // 부스 입장 통한 입장 처리
  public CheckResponseDto processQrEntry(QrTicket qrTicket) {
    return processBoothCheck(qrTicket);
  }

  // 부스 입장 시 QR 티켓 검증
  public QrTicket validateQrTicket(Users user, BoothEntryRequestDto dto,
      BoothExperienceReservation reservation) {
    QrTicket qrTicket =
        dto.getQrCode() != null ? qrTicketRepository.findByQrCode(dto.getQrCode()).orElseThrow(
            () -> new CustomException(HttpStatus.NOT_FOUND, "유효하지 않은 QR 코드입니다.")
        ) : qrTicketRepository.findByManualCode(dto.getManualCode()).orElseThrow(
            () -> new CustomException(HttpStatus.NOT_FOUND, "유효하지 않은 수동 코드입니다.")
        );
    QrActionCode qrActionCode = qrActionCodeRepository.findByCode(QrActionCode.SCANNED).orElseThrow(
        () -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "유효하지 않은 QR ACTION CODE입니다.")
    );
    qrLogService.scannedQrLog(qrTicket, qrActionCode);
    Attendee attendee = qrTicket.getAttendee();
    Reservation eventReservation = attendee.getReservation();
    if (!reservation.getUser().getUserId().equals(user.getUserId())) {
      throw new CustomException(HttpStatus.FORBIDDEN, "QR 티켓 소유자의 예약과 로그인 사용자가 일치하지 않습니다.");
    }
    if(eventReservation.getEvent().getEventId().equals(dto.getEventId())) {
      throw new CustomException(HttpStatus.BAD_REQUEST,"요청한 행사와 QR 티켓의 행사가 일치하지 않습니다");
    }
    log.info("부스 입장 시 QR 티켓 검증 완료. scan log 저장");
    return qrTicket;
  }

  /**
   * 체크인 공통 로직
   */
  private CheckResponseDto processCheckInCommon(QrTicket qrTicket, CheckInRequestDto dto) {
    // QrActionCode 검토
    QrActionCode qrActionCode = qrEntryValidateService.validateQrActionCode(QrActionCode.SCANNED);
    // 코드 스캔 기록
    qrLogService.scannedQrLog(qrTicket, qrActionCode);
    log.info("qrActionCode : {}", qrActionCode);
    log.info("QrLog SCANNED 저장 qrTicket: {}",qrTicket.getId());
    // 체크인이 가능한가 -> 규칙상 (체크인 스캔 자체가 가능한지 판단)
    qrEntryValidateService.checkEntryExitPolicy(qrTicket, QrCheckStatusCode.ENTRY);
    // 현재 입장이 최초입장인가 재입장인가
    // 현재 입장이 최초입장인지 재입장인지 판단 QrCheckStatusCode.ENTRY, rCheckStatusCode.REENTRY
    String entryType = qrLogService.determineEntryOrReEntry(qrTicket);
    log.info("현재 입장 상태:{}",entryType);
    // 재입장 가능 여부 판단
    qrEntryValidateService.verifyCheckInReEntry(qrTicket);
    // 중복 스캔?
    // 중복 스캔 -> QrLog: invalid, QrChecktLog: duplicate 저장
    qrEntryValidateService.preventDuplicateScan(qrTicket, entryType);
    log.info("중복 스캔 여부 통과");
    // 스캔 동작 순서 맞는지
    // 코드 비교
    if (QR.equals(dto.getCodeType())) {
      log.info("체크인 타입:QR");
      qrTicketVerificationService.verifyQrCode(qrTicket, dto.getCodeValue());
    } else {
      log.info("체크인 타입:MANUAL");
      qrTicketVerificationService.verifyManualCode(qrTicket, dto.getCodeValue());
    }
    log.info("QR 티켓 처리 시작");
    // QR 티켓 처리
    LocalDateTime checkInTime = processCheckIn(qrTicket, dto.getQrActionCode(), entryType);
    log.info("QR 티켓 처리 완료");
    checkInQrTicket(qrTicket);
    return CheckResponseDto.builder()
        .message("체크인 완료되었습니다.")
        .checkInTime(checkInTime)
        .build();
  }

  // 검증 완료 후 디비 데이터 저장
  private LocalDateTime processCheckIn(QrTicket qrTicket, String checkInType, String entryType) {
    log.info("qrTicket : {} DB 저장 시작",qrTicket.getId());
    // actionCodeType = CHECKED_IN, MANUAL_CHECKED_IN
    QrActionCode qrActionCode = qrEntryValidateService.validateQrActionCode(checkInType);
    // checkStatusCode = ENTRY, REENTRY
    QrCheckStatusCode qrCheckStatusCode = qrEntryValidateService.validateQrCheckStatusCode(
        entryType);
    log.info("QrActionCode : {} , QrCheckStatusCode : {}", qrActionCode, qrCheckStatusCode);
    // 잘못된 입퇴장 스캔 -> ENTRY, REENTRY
    qrEntryValidateService.preventInvalidScan(qrTicket, qrCheckStatusCode.getCode());
    log.info("잘뭇된 입퇴장 스캔 여부 통과");
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

  public void checkInQrTicket(QrTicket qrTicket){
    messagingTemplate.convertAndSend("/topic/check-in/"+qrTicket.getId(),"체크인 처리가 완료되었습니다.");
  }

  public void boothCheckIn(QrTicket qrTicket){
    messagingTemplate.convertAndSend("/topic/booth/qr/"+qrTicket.getId(),"체크인 처리가 완료되었습니다.");
  }

  private CheckResponseDto processBoothCheck(QrTicket qrTicket) {
    // 1. 현재 정책에서 필요한 이전 QR 체크 상태
    QrCheckStatusCode requiredPreviousStatus = qrEntryValidateService.determineRequiredQrStatusForBoothEntry(
        qrTicket);
    CheckResponseDto checkResponseDto;
    if(requiredPreviousStatus == null) {
      checkResponseDto = CheckResponseDto.builder()
          .message("이전 기록이 저장되어 있거나 저장할 필요가 없으므로 추가 저장 안함")
          .checkInTime(null)
          .build();
    } else{
      // 3. 부스 입장 로그 생성
      LocalDateTime checkInTime = qrLogService.boothQrLog(qrTicket, requiredPreviousStatus);
      checkResponseDto = CheckResponseDto.builder()
          .message("이전 로그 저장 완료")
          .checkInTime(checkInTime)
          .build();
    }
    boothCheckIn(qrTicket);
    return checkResponseDto;
  }
}
