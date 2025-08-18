package com.fairing.fairplay.shareticket.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.common.exception.LinkExpiredException;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import com.fairing.fairplay.shareticket.dto.ShareTicketInfoResponseDto;
import com.fairing.fairplay.shareticket.dto.TokenResponseDto;
import com.fairing.fairplay.shareticket.entity.ShareTicket;
import com.fairing.fairplay.shareticket.repository.ShareTicketRepository;
import java.time.LocalDateTime;
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

    ShareTicket shareTicket = shareTicketRepository.findByReservation(reservation).orElseThrow(
        () -> new CustomException(HttpStatus.NOT_FOUND,"해당 예약에 조회되는 참석자 등록 폼은 없습니다.")
    );

    if(!reservation.getUser().getUserId().equals(userDetails.getUserId())) {
      throw new CustomException(HttpStatus.FORBIDDEN,"해당 예약에 대한 권한이 없습니다.");
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
      throw new LinkExpiredException("해당 링크는 더 이상 유효하지 않습니다.", "/link-expired");
    }

    if (shareTicket.isFull()) {
      throw new LinkExpiredException("참가자 등록이 이미 마감되었습니다.", "/link-closed");
    }
    return shareTicket;
  }

  public void updateShareTicket(ShareTicket shareTicket) {
    shareTicket.increaseSubmittedCount();

    if (shareTicket.getSubmittedCount() >= shareTicket.getTotalAllowed()) {
      shareTicket.setExpired(true);
    }
  }
}
