package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.attendee.entity.AttendeeTypeCode;
import com.fairing.fairplay.attendee.repository.AttendeeRepository;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.event.entity.EventDetail;
import com.fairing.fairplay.qr.dto.CheckInResponseDto;
import com.fairing.fairplay.qr.dto.EntryPolicyDto;
import com.fairing.fairplay.qr.dto.GuestManualCheckInRequestDto;
import com.fairing.fairplay.qr.dto.GuestQrCheckInRequestDto;
import com.fairing.fairplay.qr.dto.MemberManualCheckInRequestDto;
import com.fairing.fairplay.qr.dto.MemberQrCheckInRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketRequestDto;
import com.fairing.fairplay.qr.entity.QrCheckLog;
import com.fairing.fairplay.qr.entity.QrCheckStatusCode;
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

//QrTicketEntryService	입장/퇴장 요청 처리의 “흐름 제어”
//    - 유저/참석자/티켓 조회
//- 권한 검증
//- 중복 스캔·재입장 정책 적용
//- 검증 통과 후 LogService 호출
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
  public CheckInResponseDto checkIn(MemberQrCheckInRequestDto dto, CustomUserDetails userDetails) {
    // 예약자 조회
    Attendee attendee = findAttendeeByReservation(dto.getReservationId());
    // 예약자와 현재 로그인한 사용자 일치 여부 조회
    if (!userDetails.getUserId().equals(attendee.getReservation().getUser().getUserId())) {
      throw new CustomException(HttpStatus.UNAUTHORIZED, "예약자와 현재 로그인한 사용자가 일치하지 않습니다.");
    }
    // QR 티켓 조회
    QrTicket qrTicket = findQrTicket(attendee);
    // 재입장 여부 검토
    verifyReEntry(qrTicket);
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
    verifyReEntry(qrTicket);
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
    // 재입장 여부 검토
    verifyReEntry(qrTicket);
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
    // 재입장 여부 검토
    verifyReEntry(qrTicket);
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
    // 입장 스캔 여부, 퇴장 스캔 여부, 재입장 가능 여부 정책 dto
    EntryPolicyDto entryPolicyDto = buildEntryPolicy(qrTicket);

    if (entryPolicyDto.getReentryAllowed()) {
      canReEntry(qrTicket, entryPolicyDto);
    } else {
      cantReEntry(qrTicket, entryPolicyDto);
    }
  }

  private void canReEntry(QrTicket qrTicket, EntryPolicyDto entryPolicyDto) {
    boolean hasEntry = qrLogService.hasCheckRecord(qrTicket, QrCheckStatusCode.ENTRY);
    boolean hasExit = qrLogService.hasCheckRecord(qrTicket, QrCheckStatusCode.EXIT);
    boolean hasReEntry = qrLogService.hasCheckRecord(qrTicket, QrCheckStatusCode.REENTRY);

    // 입장스캔O 퇴장스캔0 입장기록있음
    if (entryPolicyDto.getCheckInAllowed() && entryPolicyDto.getCheckOutAllowed()
        && hasEntry && !hasExit) {
      throw new CustomException(HttpStatus.UNAUTHORIZED, "퇴장 처리가 되지 않아 재입장하실 수 없습니다.");
    } else if (!entryPolicyDto.getCheckInAllowed() && entryPolicyDto.getCheckOutAllowed()
        && !hasExit) {
      // 입장스캔X 퇴장스캔0 퇴장 기록 없음
      throw new CustomException(HttpStatus.UNAUTHORIZED, "퇴장 처리가 되지 않아 재입장하실 수 없습니다.");
    } else if (entryPolicyDto.getCheckInAllowed() && !entryPolicyDto.getCheckOutAllowed()
        && !(hasEntry || hasReEntry)) {
      // 입장스캔O 퇴장스캔X 입장 기록 없음
      throw new CustomException(HttpStatus.UNAUTHORIZED, "입장 처리가 되지 않아 재입장하실 수 없습니다.");
    }
  }

  private void cantReEntry(QrTicket qrTicket, EntryPolicyDto entryPolicyDto) {
    boolean hasEntry = qrLogService.hasCheckRecord(qrTicket, QrCheckStatusCode.ENTRY);
    boolean hasExit = qrLogService.hasCheckRecord(qrTicket, QrCheckStatusCode.EXIT);

    // 입장스캔O 퇴장스캔0 입장기록있음
    if ((entryPolicyDto.getCheckInAllowed() && entryPolicyDto.getCheckOutAllowed() && hasEntry)
        || (!entryPolicyDto.getCheckInAllowed() && entryPolicyDto.getCheckOutAllowed() && hasExit)
        || (entryPolicyDto.getCheckInAllowed() && !entryPolicyDto.getCheckOutAllowed() && hasEntry)) {
      throw new CustomException(HttpStatus.UNAUTHORIZED, "이미 입장 또는 퇴장 완료된 티켓이므로 재입장하실 수 없습니다.");
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
  private void qrLogSave(QrTicket qrTicket, String type) {
//    qrLogService(qrTicket, type);
  }

  private EntryPolicyDto buildEntryPolicy(QrTicket qrTicket) {
    EventDetail eventDetail = qrTicket.getEventSchedule().getEvent().getEventDetail();

    // 입장 스캔 여부, 퇴장 스캔 여부, 재입장 가능 여부 정책 dto
    return EntryPolicyDto.builder()
        .checkInAllowed(true)
        .checkOutAllowed(eventDetail.getCheckOutAllowed())
        .reentryAllowed(eventDetail.getReentryAllowed())
        .build();
  }
}
