package com.fairing.fairplay.attendeeform.service;

import com.fairing.fairplay.attendeeform.dto.AttendeeFormInfoResponseDto;
import com.fairing.fairplay.attendeeform.entity.AttendeeForm;
import com.fairing.fairplay.attendeeform.repository.AttendeeFormRepository;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.common.exception.LinkExpiredException;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import com.fairing.fairplay.attendeeform.dto.TokenResponseDto;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendeeFormService {

  private final AttendeeFormRepository attendeeFormRepository;
  private final ReservationRepository reservationRepository;

  private static final String RESERVATION = "RESERVATION";
  private static final String COMPLETED = "COMPLETED";

  // 폼 링크 조회
  public TokenResponseDto getFormLink(CustomUserDetails userDetails, Long reservationId) {
    if (userDetails == null) {
      throw new CustomException(HttpStatus.UNAUTHORIZED, "폼 링크를 조회할 권한이 없습니다. 로그인해주세요.");
    }

    Reservation reservation = reservationRepository.findById(reservationId).orElseThrow(
        () -> new CustomException(HttpStatus.NOT_FOUND, "예약이 조회되지 않습니다.")
    );

    if(!reservation.getUser().getUserId().equals(userDetails.getUserId())) {
      throw new CustomException(HttpStatus.FORBIDDEN,"해당 예약에 대한 권한이 없습니다.");
    }

    AttendeeForm attendeeForm = attendeeFormRepository.findByReservation(reservation).orElse(null);
    
    // attendeeform이 없으면 자동 생성
    if (attendeeForm == null) {
      log.info("AttendeeForm이 없어 자동 생성 - reservationId: {}, quantity: {}",
          reservationId, reservation.getQuantity());
      
      // 단건 예약(quantity = 1)이면 폼 링크가 필요하지 않음
      if (reservation.getQuantity() <= 1) {
        throw new CustomException(HttpStatus.BAD_REQUEST, "단건 예약은 참석자 등록 폼이 필요하지 않습니다.");
      }
      
      attendeeForm = createAttendeeFormForReservation(reservation);
    }

    if (attendeeForm.getExpired()) {
      throw new CustomException(HttpStatus.FORBIDDEN, "링크가 만료되었습니다.");
    }

    return TokenResponseDto.builder()
        .token(attendeeForm.getLinkToken())
        .build();
  }

  public AttendeeFormInfoResponseDto getFormInfo(String token) {
    AttendeeForm attendeeForm = validateAndUseToken(token);

    if (!attendeeForm.getLinkToken().equals(token)) {
      throw new CustomException(HttpStatus.BAD_REQUEST, "잘못된 폼 링크입니다.");
    }

    Event event = attendeeForm.getReservation().getEvent();
    if (event == null) {
      throw new CustomException(HttpStatus.NOT_FOUND, "적절한 행사가 조회되지 않습니다.");
    }

    return AttendeeFormInfoResponseDto.builder()
        .formId(attendeeForm.getId())
        .eventId(event.getEventId())
        .eventName(event.getTitleKr())
        .build();
  }

  // 공유폼 token 유효성 검사
  public AttendeeForm validateAndUseToken(String token) {
    AttendeeForm attendeeForm = attendeeFormRepository.findByLinkToken(token)
        .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "요청하신 링크를 찾을 수 없습니다."));

    if (attendeeForm.getExpired()) {
      throw new LinkExpiredException("해당 링크는 더 이상 유효하지 않습니다.");
    }

    if (attendeeForm.isFull()) {
      throw new LinkExpiredException("참가자 등록이 이미 마감되었습니다.");
    }
    return attendeeForm;
  }

  public void updateAttendeeForm(AttendeeForm attendeeForm) {
    attendeeForm.increaseSubmittedCount();

    if (attendeeForm.getSubmittedCount() >= attendeeForm.getTotalAllowed()) {
      attendeeForm.setExpired(true);
    }
  }

  // 예약에 대한 AttendeeForm 자동 생성
  private AttendeeForm createAttendeeFormForReservation(Reservation reservation) {
    // 토큰 생성 (중복 방지)
    String token;
    do {
      UUID uuid = UUID.randomUUID();
      byte[] bytes = new byte[16];
      ByteBuffer.wrap(bytes)
          .putLong(uuid.getMostSignificantBits())
          .putLong(uuid.getLeastSignificantBits());
      token = Base64.getUrlEncoder().withoutPadding()
          .encodeToString(bytes);
    } while (attendeeFormRepository.existsByLinkToken(token));

    // 만료 시간 계산
    LocalDate scheduleDate = reservation.getSchedule().getDate();
    LocalDate today = LocalDateTime.now().toLocalDate();

    LocalDateTime expiredAt;
    // 행사 시작일이 오늘 기준 -1일 또는 당일일 경우 => 공유폼 만료 기한 당일 시작시간까지
    if (today.equals(scheduleDate.minusDays(1)) || today.equals(scheduleDate)) {
      expiredAt = LocalDateTime.of(scheduleDate, reservation.getSchedule().getStartTime());
    } else {
      // 그외 행사 시작 전날
      expiredAt = scheduleDate.minusDays(1).atStartOfDay();
    }

    // AttendeeForm 생성
    AttendeeForm attendeeForm = AttendeeForm.builder()
        .linkToken(token)
        .totalAllowed(reservation.getQuantity())
        .expired(false)
        .submittedCount(1) // 대표자 이미 제출됨 (기존 attendee가 있다고 가정)
        .reservation(reservation)
        .expiredAt(expiredAt)
        .build();

    log.info("AttendeeForm 자동 생성 완료 - reservationId: {}, token: {}",
        reservation.getReservationId(), token);

    return attendeeFormRepository.save(attendeeForm);
  }
}
