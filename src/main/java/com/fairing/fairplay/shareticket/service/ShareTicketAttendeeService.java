package com.fairing.fairplay.shareticket.service;

import com.fairing.fairplay.attendee.dto.AttendeeInfoResponseDto;
import com.fairing.fairplay.attendee.dto.AttendeeSaveRequestDto;
import com.fairing.fairplay.attendee.service.AttendeeService;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import com.fairing.fairplay.shareticket.dto.ShareTicketSaveRequestDto;
import com.fairing.fairplay.shareticket.dto.ShareTicketSaveResponseDto;
import com.fairing.fairplay.shareticket.entity.ShareTicket;
import com.fairing.fairplay.shareticket.repository.ShareTicketRepository;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShareTicketAttendeeService {
  private final UserRepository userRepository;
  private final AttendeeService attendeeService;
  private final ShareTicketRepository shareTicketRepository;
  private final ReservationRepository reservationRepository;

  // 대표자 저장 및 참석자 폼 링크 생성해 반환
  @Transactional
  public ShareTicketSaveResponseDto saveShareTicketAndAttendee(CustomUserDetails userDetails,
      ShareTicketSaveRequestDto dto) {
    if(userDetails == null || dto.getReservationId() == null) {
      throw new CustomException(HttpStatus.UNAUTHORIZED,"로그인 후 이용해주세요");
    }

    // 1. attendee 저장
    Users buyUser = userRepository.findByUserId(userDetails.getUserId()).orElseThrow(
        () -> new CustomException(HttpStatus.UNAUTHORIZED, "회원을 조회할 수 없습니다.")
    );

    Reservation reservation = reservationRepository.findById(dto.getReservationId()).orElseThrow(
        () -> new CustomException(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다.")
    );

    if(!reservation.getUser().getUserId().equals(buyUser.getUserId())) {
      throw new CustomException(HttpStatus.FORBIDDEN,"해당 예약에 대한 권한이 없습니다.");
    }

    AttendeeSaveRequestDto attendeeSaveRequestDto = AttendeeSaveRequestDto.builder()
        .name(buyUser.getName())
        .email(buyUser.getEmail())
        .phone(buyUser.getPhone())
        .agreeToTerms(true)
        .build();
    AttendeeInfoResponseDto attendeeInfoResponseDto = attendeeService.savePrimary(
        attendeeSaveRequestDto, dto.getReservationId());

    // 2. shareTicket 저장 -> 단건 예매면 token은 Null
    String token = dto.getTotalAllowed() > 1 ? generateToken(dto) : null;

    return ShareTicketSaveResponseDto.builder()
        .reservationId(dto.getReservationId())
        .token(token)
        .build();
  }

  // 공유 폼 링크 생성 -> 예약 성공 시 예약 서비스 단계에서 사용
  private String generateToken(ShareTicketSaveRequestDto dto) {
    if (dto == null) {
      throw new CustomException(HttpStatus.NOT_FOUND,"유효하지 않은 요청입니다.");
    }

    // 예약 유무 조회
    Reservation reservation = reservationRepository.findById(dto.getReservationId())
        .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다."));

    // 예약 ID 기준 폼이 생성되어있는지 조회
    if (shareTicketRepository.existsByReservation_ReservationId(dto.getReservationId())) {
      throw new CustomException(HttpStatus.CONFLICT, "이미 폼 링크가 생성되었습니다.");
    }

    String token;
    do {
      UUID uuid = UUID.randomUUID();
      byte[] bytes = new byte[16];
      ByteBuffer.wrap(bytes)
          .putLong(uuid.getMostSignificantBits())
          .putLong(uuid.getLeastSignificantBits());
      token = Base64.getUrlEncoder().withoutPadding()
          .encodeToString(bytes); //5B3Ej0AdRMqrqY7xV6k9tw 형태
    } while (shareTicketRepository.existsByLinkToken(token));

    if (dto.getTotalAllowed() == null || dto.getTotalAllowed() <= 1) {
      throw new CustomException(HttpStatus.BAD_REQUEST, "허용 인원은 2명 이상이어야 합니다.");
    }

    LocalDate scheduleDate = reservation.getSchedule().getDate();
    LocalDate today = LocalDateTime.now().toLocalDate();

    LocalDateTime expiredAt;
    if (today.equals(
        scheduleDate.minusDays(1))) {
      expiredAt = scheduleDate.atStartOfDay();
    } else {
      expiredAt = scheduleDate.minusDays(1).atStartOfDay();
    }

    ShareTicket shareTicket = ShareTicket.builder()
        .linkToken(token) // 폼 링크 토큰
        .totalAllowed(dto.getTotalAllowed()) //대표자 제출 O
        .expired(false) // 만료 여부
        .submittedCount(1) // 대표자 제출
        .reservation(reservation) // 예약 연결
        .expiredAt(expiredAt) // 폼 만료 기한 (행사 시작일 하루 전)
        .build();

    shareTicketRepository.saveAndFlush(shareTicket);
    return token;
  }
}
