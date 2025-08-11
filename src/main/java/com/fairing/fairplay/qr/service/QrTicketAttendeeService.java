package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.attendee.entity.AttendeeTypeCode;
import com.fairing.fairplay.attendee.repository.AttendeeRepository;
import com.fairing.fairplay.attendee.repository.AttendeeTypeCodeRepository;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.qr.dto.QrTicketRequestDto;
import com.fairing.fairplay.qr.entity.QrTicket;
import com.fairing.fairplay.qr.repository.QrTicketRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/*
 * QR 티켓&회원 조회 클래스
 * */
@Component
@RequiredArgsConstructor
@Slf4j
public class QrTicketAttendeeService {

  private final AttendeeRepository attendeeRepository;
  private final AttendeeTypeCodeRepository attendeeTypeCodeRepository;
  private final QrTicketRepository qrTicketRepository;

  public QrTicket load(QrTicketRequestDto dto, Integer attendeeTypeCodeId) {
    Attendee attendee = loadAttendee(dto, attendeeTypeCodeId);

    return qrTicketRepository.findByAttendee(attendee)
        .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "QR 티켓을 찾을 수 없습니다."));
  }

  /**
   * Attendee 조회 (회원은 reservationId, 비회원은 attendeeId로)
   */
  public Attendee findAttendee(Long reservationId, Long attendeeId) {
    if (reservationId != null) {
      return findAttendeeByReservation(reservationId);
    }
    if (attendeeId != null) {
      return findAttendeeByAttendee(attendeeId);
    }
    throw new CustomException(HttpStatus.BAD_REQUEST, "reservationId 또는 attendeeId 중 하나는 필수입니다.");
  }

  // 회원 attendee 조회
  public Attendee findAttendeeByReservation(Long reservationId) {
    return attendeeRepository.findByReservation_ReservationIdAndAttendeeTypeCode_Code(
            reservationId, AttendeeTypeCode.PRIMARY)
        .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "참석자를 조회하지 못했습니다."));
  }

  // 비회원 attendee 조회
  public Attendee findAttendeeByAttendee(Long attendeeId) {
    return attendeeRepository.findById(attendeeId)
        .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "참석자를 조회하지 못했습니다."));
  }

  public Attendee loadAttendee(QrTicketRequestDto dto, Integer attendeeTypeCodeId) {
    if (attendeeTypeCodeId == null) {
      return loadWithoutType(dto);
    } else {
      return loadWithType(dto, attendeeTypeCodeId);
    }
  }

  public AttendeeTypeCode findPrimaryTypeCode() {
    return attendeeTypeCodeRepository.findByCode(AttendeeTypeCode.PRIMARY).orElseThrow(
        () -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "참석자 타입 기본 설정이 올바르지 않습니다. DB 확인이 필요합니다.")
    );
  }

  public AttendeeTypeCode findGuestTypeCode() {
    return attendeeTypeCodeRepository.findByCode(AttendeeTypeCode.GUEST).orElseThrow(
        () -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "참석자 타입 기본 설정이 올바르지 않습니다. DB 확인이 필요합니다.")
    );
  }

  private Attendee loadWithType(QrTicketRequestDto dto, Integer typeCodeId) {
    AttendeeTypeCode attendeeTypeCode = attendeeTypeCodeRepository.findById(typeCodeId).orElseThrow(
        () -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "참석자 타입 기본 설정이 올바르지 않습니다. DB 확인이 필요합니다.")
    );

    if (Objects.equals(attendeeTypeCode.getCode(), AttendeeTypeCode.PRIMARY)) {
      return attendeeRepository.findByReservation_ReservationIdAndAttendeeTypeCode_Id(
              dto.getReservationId(), attendeeTypeCode.getId())
          .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "대표 참석자를 찾을 수 없습니다."));
    } else if (Objects.equals(attendeeTypeCode.getCode(), AttendeeTypeCode.GUEST)) {
      if (dto.getAttendeeId() == null) {
        throw new CustomException(HttpStatus.BAD_REQUEST, "동반 참석자 ID가 누락되었습니다.");
      }
      return attendeeRepository.findByIdAndReservation_ReservationIdAndAttendeeTypeCode_Id(
              dto.getAttendeeId(), dto.getReservationId(), attendeeTypeCode.getId())
          .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "동반 참석자를 찾을 수 없습니다."));
    } else {
      throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "알 수 없는 참석자 유형입니다. DB 확인이 필요합니다.");
    }
  }

  private Attendee loadWithoutType(QrTicketRequestDto dto) {
    AttendeeTypeCode attendeeTypeCode = findPrimaryTypeCode();
    if (dto.getAttendeeId() != null) {
      return attendeeRepository.findById(dto.getAttendeeId())
          .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "참석자 ID로 참석자를 찾을 수 없습니다."));
    } else if (dto.getReservationId() != null) {
      return attendeeRepository.findByReservation_ReservationIdAndAttendeeTypeCode_Id(
              dto.getReservationId(), attendeeTypeCode.getId())
          .orElseThrow(
              () -> new CustomException(HttpStatus.NOT_FOUND, "예약 ID로 대표 참석자를 찾을 수 없습니다."));
    } else {
      throw new CustomException(HttpStatus.BAD_REQUEST, "참석자 조회에 필요한 정보가 부족합니다.");
    }
  }
}
