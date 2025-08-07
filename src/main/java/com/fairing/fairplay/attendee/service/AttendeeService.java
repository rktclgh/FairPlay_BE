package com.fairing.fairplay.attendee.service;

import com.fairing.fairplay.attendee.dto.AttendeeInfoResponseDto;
import com.fairing.fairplay.attendee.dto.AttendeeListInfoResponseDto;
import com.fairing.fairplay.attendee.dto.AttendeeSaveRequestDto;
import com.fairing.fairplay.attendee.dto.AttendeeUpdateRequestDto;
import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.attendee.entity.AttendeeTypeCode;
import com.fairing.fairplay.attendee.repository.AttendeeRepository;
import com.fairing.fairplay.attendee.repository.AttendeeTypeCodeRepository;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import com.fairing.fairplay.shareticket.entity.ShareTicket;
import com.fairing.fairplay.shareticket.service.ShareTicketService;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*참석자 서비스*/
@Service
@RequiredArgsConstructor
public class AttendeeService {

  private final AttendeeRepository attendeeRepository;
  private final AttendeeTypeCodeRepository attendeeTypeCodeRepository;
  private final ShareTicketService shareTicketService;
  private final ReservationRepository reservationRepository;

  // 대표자 정보 저장
  @Transactional
  public AttendeeInfoResponseDto savePrimary(AttendeeSaveRequestDto dto, Long reservationId) {
    return saveAttendee("PRIMARY", dto, reservationId);
  }

  // 동반자 정보 저장
  @Transactional
  public AttendeeInfoResponseDto saveGuest(String token, AttendeeSaveRequestDto dto) {
    ShareTicket shareTicket = shareTicketService.validateAndUseToken(token);
    AttendeeInfoResponseDto attendeeInfoResponseDto = saveAttendee("GUEST", dto,
        shareTicket.getReservation().getReservationId());
    shareTicketService.updateShareTicket(shareTicket);
    return attendeeInfoResponseDto;
  }

  // 참석자 전체 조회
  public AttendeeListInfoResponseDto findAll(Long reservationId, CustomUserDetails userDetails) {
    Reservation reservation = findReservation(reservationId);

    Long userId = userDetails.getUserId();

    // 대표자와 현재 수정 요청한 요청자의 ID 비교
    if (!Objects.equals(reservation.getUser().getUserId(), userId)) {
      throw new CustomException(HttpStatus.FORBIDDEN, "현재 요청한 요청자와 대표자가 일치하지 않습니다.");
    }

    List<Attendee> attendees = attendeeRepository.findAllByReservation_ReservationIdOrderByIdAsc(
        reservationId);

    List<AttendeeInfoResponseDto> result = attendees.stream()
        .map(attendee -> AttendeeInfoResponseDto.builder()
            .attendeeId(attendee.getId())
            .name(attendee.getName())
            .email(attendee.getEmail())
            .phone(attendee.getPhone())
            .build())
        .toList();

    return AttendeeListInfoResponseDto.builder()
        .reservationId(reservationId)
        .attendees(result)
        .build();
  }

  // 동반자 정보 수정
  @Transactional
  public AttendeeInfoResponseDto updateAttendee(Long attendeeId, AttendeeUpdateRequestDto dto,
      CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();

    // 예약 있는지 조회
    Reservation reservation = findReservation(dto.getReservationId());

    // 대표자와 현재 수정 요청한 요청자의 ID 비교
    if (!Objects.equals(reservation.getUser().getUserId(), userId)) {
      throw new CustomException(HttpStatus.FORBIDDEN, "현재 요청한 요청자와 대표자가 일치하지 않습니다.");
    }

    // 참석자 정보 조회 -> 예약ID + 참석자ID
    Attendee attendee = attendeeRepository.findByIdAndReservation_ReservationId(attendeeId,
            reservation.getReservationId())
        .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "참석자 정보를 조회할 수 없습니다."));

    // 수정하려는 정보가 대표자일 경우 수정 불가하므로 예외 발생
    if ("PRIMARY".equals(attendee.getAttendeeTypeCode().getCode().trim())) {
      throw new CustomException(HttpStatus.FORBIDDEN, "대표자 정보는 수정할 수 없습니다.");
    }

    // 정보 변경
    attendee.setEmail(dto.getEmail());
    attendee.setPhone(dto.getPhone());
    attendee.setName(dto.getName());

    return AttendeeInfoResponseDto.builder()
        .attendeeId(attendee.getId())
        .reservationId(attendee.getReservation().getReservationId())
        .name(attendee.getName())
        .email(attendee.getEmail())
        .phone(attendee.getPhone())
        .build();
  }

  // 행사별 예약자 명단 조회 (행사 관리자)
  public List<Attendee> getAttendeesByEvent(Long eventId, Long userId) {
    // eventId 유효성 검사
    if (eventId == null || eventId <= 0) {
      throw new CustomException(HttpStatus.BAD_REQUEST, "유효하지 않은 행사 ID입니다.");
    }

    // 행사 관리자 권한 검증

    return attendeeRepository.findByEventId(eventId);
  }

  // DB save 로직 분리
  private AttendeeInfoResponseDto saveAttendee(String attendeeType, AttendeeSaveRequestDto dto,
      Long reservationId) {
    AttendeeTypeCode attendeeTypeCode = attendeeTypeCodeRepository.findByCode(attendeeType)
        .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 타입입니다."));

    Reservation reservation = findReservation(reservationId);

    Attendee attendee = Attendee.builder()
        .name(dto.getName())
        .attendeeTypeCode(attendeeTypeCode)
        .phone(dto.getPhone())
        .email(dto.getEmail())
        .reservation(reservation)
        .build();
    Attendee savedAttendee = attendeeRepository.save(attendee);
    return AttendeeInfoResponseDto.builder()
        .attendeeId(savedAttendee.getId())
        .reservationId(savedAttendee.getReservation().getReservationId())
        .name(savedAttendee.getName())
        .email(savedAttendee.getEmail())
        .phone(savedAttendee.getPhone())
        .build();
  }

  private Reservation findReservation(Long reservationId) {
    return reservationRepository.findById(reservationId).orElseThrow(
        () -> new CustomException(HttpStatus.NOT_FOUND, "예약 정보를 조회할 수 없습니다."));
  }

  private void checkReservation(Long reservationId) {
    if (!reservationRepository.existsById(reservationId)) {
      throw new CustomException(HttpStatus.NOT_FOUND, "예약 정보를 조회할 수 없습니다.");
    }
  }
}
