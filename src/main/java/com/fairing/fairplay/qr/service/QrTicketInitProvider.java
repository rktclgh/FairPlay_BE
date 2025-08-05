package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.attendee.entity.Attendee;

import com.fairing.fairplay.attendee.repository.AttendeeRepository;
import com.fairing.fairplay.common.exception.CustomException;

import com.fairing.fairplay.qr.dto.QrTicketRequestDto;
import com.fairing.fairplay.qr.entity.QrTicket;
import com.fairing.fairplay.qr.repository.QrTicketRepository;
import com.fairing.fairplay.qr.repository.QrTicketRepositoryCustom;
import com.fairing.fairplay.qr.util.CodeGenerator;

import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/*
 * QR 티켓 객체 생성 클래스
 * */
@Component
@RequiredArgsConstructor
@Slf4j
public class QrTicketInitProvider {

  private final AttendeeRepository attendeeRepository;
  private final QrTicketRepository qrTicketRepository;


  // 저장된 QR 티켓 조회
  public QrTicket load(QrTicketRequestDto dto, Integer attendeeTypeCodeId) {
    Attendee attendee;
    if (attendeeTypeCodeId == null) {
      attendee = loadAttendeeWithoutTypeCheck(dto);
    } else {
      attendee = loadAttendeeWithTypeCheck(dto, attendeeTypeCodeId);
    }

    return qrTicketRepository.findByAttendee(attendee)
        .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "QR 티켓을 조회할 수 없습니다."));
  }

  private Attendee loadAttendeeWithTypeCheck(QrTicketRequestDto dto, Integer attendeeTypeCodeId) {
    if (attendeeTypeCodeId == 1) {
      return attendeeRepository.findByReservation_ReservationIdAndAttendeeTypeCode_Id(
              dto.getReservationId(), attendeeTypeCodeId)
          .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "참석자를 조회할 수 없습니다."));
    } else if (attendeeTypeCodeId == 2) {
      if (dto.getAttendeeId() == null) {
        throw new CustomException(HttpStatus.BAD_REQUEST, "대표자가 아니므로 조회할 수 없습니다.");
      }
      return attendeeRepository.findByIdAndReservation_ReservationIdAndAttendeeTypeCode_Id(
              dto.getAttendeeId(), dto.getReservationId(), attendeeTypeCodeId)
          .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "동반 참석자를 조회할 수 없습니다."));
    } else {
      throw new CustomException(HttpStatus.BAD_REQUEST, "지원하지 않는 참석자 유형입니다.");
    }
  }

  // 재발급 할 때 attendee 조회
  private Attendee loadAttendeeWithoutTypeCheck(QrTicketRequestDto dto) {
    Long reservationId = dto.getReservationId();
    if (dto.getAttendeeId() == null) {
      return attendeeRepository.findById(reservationId)
          .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "참석자를 조회할 수 없습니다."));
    } else if (dto.getReservationId() == null) {
      return attendeeRepository.findByReservation_ReservationIdAndAttendeeTypeCode_Id(reservationId,
              1)
          .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "참석자를 조회할 수 없습니다."));
    } else {
      throw new CustomException(HttpStatus.BAD_REQUEST, "참석자 조회에 필요한 정보가 부족합니다.");
    }
  }
}
