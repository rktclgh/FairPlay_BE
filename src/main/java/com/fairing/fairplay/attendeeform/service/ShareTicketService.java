package com.fairing.fairplay.attendeeform.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.common.exception.LinkExpiredException;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import com.fairing.fairplay.attendeeform.dto.ShareTicketInfoResponseDto;
import com.fairing.fairplay.attendeeform.dto.TokenResponseDto;
import com.fairing.fairplay.attendeeform.entity.ShareTicket;
import com.fairing.fairplay.attendeeform.repository.ShareTicketRepository;
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
public class ShareTicketService {

  private final ShareTicketRepository shareTicketRepository;
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

    ShareTicket shareTicket = shareTicketRepository.findByReservation(reservation).orElse(null);
    
    // ShareTicket이 없으면 자동 생성
    if (shareTicket == null) {
      log.info("ShareTicket이 없어 자동 생성 - reservationId: {}, quantity: {}", 
          reservationId, reservation.getQuantity());
      
      // 단건 예약(quantity = 1)이면 폼 링크가 필요하지 않음
      if (reservation.getQuantity() <= 1) {
        throw new CustomException(HttpStatus.BAD_REQUEST, "단건 예약은 참석자 등록 폼이 필요하지 않습니다.");
      }
      
      shareTicket = createShareTicketForReservation(reservation);
    }

    if (shareTicket.getExpired()) {
      throw new CustomException(HttpStatus.FORBIDDEN, "링크가 만료되었습니다.");
    }

    return TokenResponseDto.builder()
        .token(shareTicket.getLinkToken())
        .build();
  }

  public ShareTicketInfoResponseDto getFormInfo(String token) {
    ShareTicket shareTicket = validateAndUseToken(token);

    if (!shareTicket.getLinkToken().equals(token)) {
      throw new CustomException(HttpStatus.BAD_REQUEST, "잘못된 폼 링크입니다.");
    }

    Event event = shareTicket.getReservation().getEvent();
    if (event == null) {
      throw new CustomException(HttpStatus.NOT_FOUND, "적절한 행사가 조회되지 않습니다.");
    }

    return ShareTicketInfoResponseDto.builder()
        .formId(shareTicket.getId())
        .eventId(event.getEventId())
        .eventName(event.getTitleKr())
        .build();
  }

  // 공유폼 token 유효성 검사
  public ShareTicket validateAndUseToken(String token) {
    ShareTicket shareTicket = shareTicketRepository.findByLinkToken(token)
        .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "요청하신 링크를 찾을 수 없습니다."));

    if (shareTicket.getExpired()) {
      throw new LinkExpiredException("해당 링크는 더 이상 유효하지 않습니다.");
    }

    if (shareTicket.isFull()) {
      throw new LinkExpiredException("참가자 등록이 이미 마감되었습니다.");
    }
    return shareTicket;
  }

  public void updateShareTicket(ShareTicket shareTicket) {
    shareTicket.increaseSubmittedCount();

    if (shareTicket.getSubmittedCount() >= shareTicket.getTotalAllowed()) {
      shareTicket.setExpired(true);
    }
  }

  // 예약에 대한 ShareTicket 자동 생성
  private ShareTicket createShareTicketForReservation(Reservation reservation) {
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
    } while (shareTicketRepository.existsByLinkToken(token));

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

    // ShareTicket 생성
    ShareTicket shareTicket = ShareTicket.builder()
        .linkToken(token)
        .totalAllowed(reservation.getQuantity())
        .expired(false)
        .submittedCount(1) // 대표자 이미 제출됨 (기존 attendee가 있다고 가정)
        .reservation(reservation)
        .expiredAt(expiredAt)
        .build();

    log.info("ShareTicket 자동 생성 완료 - reservationId: {}, token: {}", 
        reservation.getReservationId(), token);

    return shareTicketRepository.save(shareTicket);
  }
}
