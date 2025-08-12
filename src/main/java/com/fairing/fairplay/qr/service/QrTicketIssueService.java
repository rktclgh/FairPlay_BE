package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.attendee.entity.AttendeeTypeCode;
import com.fairing.fairplay.attendee.repository.AttendeeRepository;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.common.exception.LinkExpiredException;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.qr.dto.QrTicketReissueGuestRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketReissueMemberRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketReissueRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketReissueResponseDto;
import com.fairing.fairplay.qr.dto.QrTicketRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketResponseDto;
import com.fairing.fairplay.qr.dto.QrTicketResponseDto.ViewingScheduleInfo;
import com.fairing.fairplay.qr.dto.QrTicketUpdateResponseDto;
import com.fairing.fairplay.qr.entity.QrTicket;
import com.fairing.fairplay.qr.repository.QrTicketRepository;
import com.fairing.fairplay.qr.util.CodeGenerator;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import com.fairing.fairplay.ticket.entity.EventSchedule;
import com.fairing.fairplay.ticket.repository.EventScheduleRepository;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// QR Ticket 발급 및 저장
@Component
@RequiredArgsConstructor
@Slf4j
public class QrTicketIssueService {

  private final QrTicketRepository qrTicketRepository;
  private final ReservationRepository reservationRepository;
  private final CodeGenerator codeGenerator;
  private final QrTicketAttendeeService qrTicketAttendeeService;
  private final QrLinkService qrLinkService;
  private final QrEmailService qrEmailService;

  private static final String ROLE_COMMON = "COMMON";
  private final EventScheduleRepository eventScheduleRepository;
  private final UserRepository userRepository;
  private final AttendeeRepository attendeeRepository;

  // 회원 QR 티켓 조회 -> 마이페이지에서 조회
  @Transactional
  public QrTicketResponseDto issueMemberTicket(QrTicketRequestDto dto,
      CustomUserDetails userDetails) {
    Reservation reservation = checkReservationBeforeNow(dto.getReservationId());

    if (!userDetails.getRoleCode().equals(ROLE_COMMON)) {
      throw new CustomException(HttpStatus.FORBIDDEN,
          "일반 사용자가 아닙니다. 현재 로그인된 사용자 권한: " + userDetails.getRoleCode());
    }

    if (!Objects.equals(userDetails.getUserId(), reservation.getUser().getUserId())) {
      throw new CustomException(HttpStatus.FORBIDDEN, "본인의 예약만 조회할 수 있습니다.");
    }

    AttendeeTypeCode attendeeTypeCode = qrTicketAttendeeService.findPrimaryTypeCode();

    QrTicket savedTicket = generateAndSaveQrTicket(dto, attendeeTypeCode.getId());
    return buildQrTicketResponse(savedTicket, reservation);
  }

  // 비회원 QR 티켓 조회 -> QR 티켓 링크 통한 조회
  @Transactional
  public QrTicketResponseDto issueGuestTicket(String token) {
    // 토큰 파싱해 예약 정보 조회
    QrTicketRequestDto dto = qrLinkService.decodeToDto(token);

    Reservation reservation = checkReservationBeforeNow(dto.getReservationId());

    AttendeeTypeCode attendeeTypeCode = qrTicketAttendeeService.findGuestTypeCode();

    // QR 티켓 조회해 qr code, manualcode 생성해서 반환
    QrTicket savedTicket = generateAndSaveQrTicket(dto, attendeeTypeCode.getId());
    // 프론트 응답
    return buildQrTicketResponse(savedTicket, reservation);
  }

  // QR 티켓 재발급 - 새로고침 버튼
  @Transactional
  public QrTicketUpdateResponseDto reissueQrTicketByGuest(QrTicketReissueGuestRequestDto dto) {
    // QR URL 디코딩
    QrTicketRequestDto qrTicketRequestDto = qrLinkService.decodeToDto(dto.getQrUrlToken());

    // 예약 여부 조회
    Reservation reservation = checkReservationBeforeNow(qrTicketRequestDto.getReservationId());

    // QR 티켓 재발급
    QrTicket savedTicket = generateAndSaveQrTicket(qrTicketRequestDto, null);
    return QrTicketUpdateResponseDto.builder().qrCode(savedTicket.getQrCode())
        .manualCode(savedTicket.getManualCode()).build();
  }

  // QR 티켓 재발급 - 새로고침 버튼. 회원
  @Transactional
  public QrTicketUpdateResponseDto reissueQrTicketByMember(QrTicketReissueMemberRequestDto dto,
      CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();

    // 예약 여부 조회
    Reservation reservation = checkReservationBeforeNow(dto.getReservationId());
    AttendeeTypeCode attendeeTypeCode = qrTicketAttendeeService.findPrimaryTypeCode();
    Attendee attendee = attendeeRepository.findByReservation_ReservationIdAndAttendeeTypeCode_Code(
        reservation.getReservationId(), attendeeTypeCode.getCode()).orElseThrow(
        () -> new CustomException(HttpStatus.NOT_FOUND, "예약자가 일치하지 않습니다. 관리자에게 문의 바랍니다."));

    if (!reservation.getUser().getUserId().equals(userId)) {
      throw new CustomException(HttpStatus.BAD_REQUEST, "예약자와 QR 티켓에 등록된 참석자가 일치하지 않습니다.");
    }

    QrTicketRequestDto qrTicketRequestDto = QrTicketRequestDto.builder()
        .attendeeId(attendee.getId())
        .reservationId(reservation.getReservationId())
        .ticketId(reservation.getTicket().getTicketId())
        .eventId(reservation.getEvent().getEventId())
        .build();

    // QR 티켓 재발급
    QrTicket savedTicket = generateAndSaveQrTicket(qrTicketRequestDto, null);
    return QrTicketUpdateResponseDto.builder().qrCode(savedTicket.getQrCode())
        .manualCode(savedTicket.getManualCode()).build();
  }


  // 관리자 강제 QR 티켓 재발급 - 마이페이지 접속 가능한 회원
  @Transactional
  public QrTicketReissueResponseDto reissueAdminQrTicketByUser(QrTicketReissueRequestDto dto) {
    QrTicket qrTicket = qrTicketRepository.findByTicketNo(dto.getTicketNo()).orElseThrow(
        () -> new CustomException(HttpStatus.NOT_FOUND,
            "해당 티켓 번호에 일치하는 QR 티켓이 존재하지 않습니다. ticketNo: " + dto.getTicketNo())
    );

    if (!Objects.equals(qrTicket.getAttendee().getId(), dto.getAttendeeId())) {
      throw new CustomException(HttpStatus.FORBIDDEN, "해당 티켓은 요청한 사용자 소유가 아닙니다.");
    }

    if (!qrTicket.getAttendee().getAttendeeTypeCode().getCode().equals("PRIMARY")) {
      throw new CustomException(HttpStatus.FORBIDDEN, "대표자가 아니므로 마이페이지에 QR 티켓을 조회하실 수 없습니다.");
    }

    // 재발급된 QR 티켓 - qrcode, manualcode null 처리
    QrTicket resetQrTicket = resetQrTicket(qrTicket);
    Attendee attendee = resetQrTicket.getAttendee();

    // 메일 전송
    qrEmailService.successSendQrEmail(attendee.getEmail(), attendee.getName());
    return buildQrTicketReissueResponse(resetQrTicket.getTicketNo(), attendee.getEmail());
  }

  // 관리자 강제 QR 티켓 링크 재발급 - 마이페이지 접속 안되는 회원/ 비회원
  @Transactional
  public QrTicketReissueResponseDto reissueAdminQrTicket(QrTicketReissueRequestDto dto) {
    QrTicket qrTicket = qrTicketRepository.findByTicketNo(dto.getTicketNo()).orElseThrow(
        () -> new CustomException(HttpStatus.NOT_FOUND,
            "해당 티켓 번호에 일치하는 QR 티켓이 존재하지 않습니다. ticketNo: " + dto.getTicketNo())
    );

    if (!Objects.equals(qrTicket.getAttendee().getId(), dto.getAttendeeId())) {
      throw new CustomException(HttpStatus.FORBIDDEN, "해당 티켓은 요청한 사용자 소유가 아닙니다.");
    }

    // 재발급된 QR 티켓 - qrcode, manualcode null 처리
    QrTicket resetQrTicket = resetQrTicket(qrTicket);
    Attendee attendee = resetQrTicket.getAttendee();
    Reservation reservation = attendee.getReservation();

    QrTicketRequestDto qrTicketRequestDto = QrTicketRequestDto.builder()
        .reservationId(reservation.getReservationId())
        .eventId(resetQrTicket.getEventSchedule().getEvent().getEventId())
        .ticketId(reservation.getTicket().getTicketId())
        .attendeeId(attendee.getId())
        .build();

    // 메일 전송
    String qrUrl = qrLinkService.generateQrLink(qrTicketRequestDto);
    qrEmailService.reissueQrEmail(qrUrl, attendee.getEmail(), attendee.getName());

    return buildQrTicketReissueResponse(resetQrTicket.getTicketNo(), attendee.getEmail());
  }

  // 행사일이 오늘보다 전날 또는 행사일이 오늘인데 종료 시간이 현재 시간보다 더 늦은 경우 예외 처리
  private Reservation checkReservationBeforeNow(Long reservationId) {
    Reservation reservation = reservationRepository.findById(reservationId)
        .orElseThrow(() -> new CustomException(
            HttpStatus.NOT_FOUND, "올바른 예약 티켓이 아닙니다."));

    LocalDate nowDate = LocalDate.now(ZoneId.of("Asia/Seoul"));
    LocalTime nowTime = LocalTime.now(ZoneId.of("Asia/Seoul"));

    if (reservation.getSchedule().getDate().isBefore(nowDate) || (
        reservation.getSchedule().getDate().isEqual(nowDate) && reservation.getSchedule()
            .getEndTime().isBefore(nowTime))) {
      throw new LinkExpiredException("종료된 행사입니다.", null);
    }

    return reservation;
  }

  // 저장된 qr 티켓 조회 후 qrcode, manualcode 발급받아 저장
  private QrTicket generateAndSaveQrTicket(QrTicketRequestDto dto, Integer type) {
    QrTicket qrTicket = findQrTicket(dto, type);
    String qrCode;
    String manualCode;
    int maxRetries = 100;
    int retryCount = 0;

    do {
      if (++retryCount > maxRetries) {
        throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "QR 코드 생성 실패: 최대 재시도 횟수 초과");
      }
      qrCode = codeGenerator.generateQrCode(qrTicket);
    } while (qrTicketRepository.existsByQrCode(qrCode));
    retryCount = 0;
    do {
      if (++retryCount > maxRetries) {
        throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "수동 코드 생성 실패: 최대 재시도 횟수 초과");
      }
      manualCode = codeGenerator.generateManualCode();
    } while (qrTicketRepository.existsByManualCode(manualCode));

    qrTicket.setQrCode(qrCode);
    qrTicket.setManualCode(manualCode);
    return qrTicketRepository.save(qrTicket);
  }

  // QR 티켓 초기화
  private QrTicket resetQrTicket(QrTicket qrTicket) {
    qrTicket.setQrCode(null);
    qrTicket.setManualCode(null);
    return qrTicketRepository.save(qrTicket);
  }

  // QR 티켓 조회
  private QrTicket findQrTicket(QrTicketRequestDto dto, Integer type) {
    return qrTicketAttendeeService.load(dto, type);
  }

  // 재발급 응답 생성
  private QrTicketReissueResponseDto buildQrTicketReissueResponse(String ticketNo, String email) {
    return QrTicketReissueResponseDto.builder()
        .ticketNo(ticketNo)
        .email(email)
        .build();
  }

  // 발급 응답 생성
  private QrTicketResponseDto buildQrTicketResponse(QrTicket qrTicket,
      Reservation reservation) {
    QrTicketResponseDto dto = qrTicketRepository.findDtoById(qrTicket.getId())
        .orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "티켓 조회 실패"));

    if (reservation.getCreatedAt() != null) {
      String formattedDate = reservation.getCreatedAt().format(
          DateTimeFormatter.ofPattern("yyyy. MM. dd"));
      dto.setReservationDate(formattedDate);
    }

    ViewingScheduleInfo viewingScheduleInfo = getViewingScheduleInfo(
        reservation.getEvent().getEventId(),
        reservation.getSchedule().getScheduleId());
    dto.setViewingScheduleInfo(viewingScheduleInfo);
    return dto;
  }

  private ViewingScheduleInfo getViewingScheduleInfo(Long eventId, Long scheduleId) {

    EventSchedule eventSchedule = eventScheduleRepository.findByEvent_EventIdAndScheduleId(eventId,
            scheduleId)
        .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "일정 정보를 찾을 수 없습니다."));
    ;

    String date = getDate(eventSchedule.getDate());
    String dayOfWeek = getDayOfWeek(eventSchedule.getWeekday());
    String startTime = getTime(eventSchedule.getStartTime());

    return ViewingScheduleInfo.builder()
        .date(date)
        .dayOfWeek(dayOfWeek)
        .startTime(startTime)
        .build();
  }

  private String getDate(LocalDate date) {
    return date.format(DateTimeFormatter.ofPattern("yyyy. MM. dd"));
  }

  private String getDayOfWeek(int weekday) {
    int dayOfWeekValue = (weekday == 0) ? 7 : weekday; // 0(일)은 7로 매핑
    return DayOfWeek.of(dayOfWeekValue).getDisplayName(TextStyle.FULL, Locale.KOREAN);
  }

  private String getTime(LocalTime time) {
    return time.format(DateTimeFormatter.ofPattern("HH:mm"));
  }

}
