package com.fairing.fairplay.shareticket.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.common.exception.LinkExpiredException;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.payment.repository.PaymentRepository;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import com.fairing.fairplay.shareticket.dto.ShareTicketInfoResponseDto;
import com.fairing.fairplay.shareticket.dto.ShareTicketSaveRequestDto;
import com.fairing.fairplay.shareticket.entity.ShareTicket;
import com.fairing.fairplay.shareticket.repository.ShareTicketRepository;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class ShareTicketService {

  private final ShareTicketRepository shareTicketRepository;
  private final ReservationRepository reservationRepository;
  private final PaymentRepository paymentRepository;

  private static final String RESERVATION = "RESERVATION";
  private static final String COMPLETED = "COMPLETED";

  @Transactional
  public String generateToken(ShareTicketSaveRequestDto dto) {
    if (dto == null) {
      throw new IllegalArgumentException("유효하지 않은 요청입니다.");
    }

    Reservation reservation = reservationRepository.findById(dto.getReservationId())
        .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다."));

    boolean hasCompleted = paymentRepository
        .existsByTargetIdAndPaymentTargetType_PaymentTargetCodeAndPaymentStatusCode_Code(
            reservation.getReservationId(), RESERVATION, COMPLETED);
    if (!hasCompleted) {
      throw new CustomException(HttpStatus.BAD_REQUEST, "결제가 정상적으로 처리되지 않았습니다.");
    }

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
          .encodeToString(bytes);
    } while (shareTicketRepository.existsByLinkToken(token));

    if (dto.getTotalAllowed() == null || dto.getTotalAllowed() <= 0) {
      throw new CustomException(HttpStatus.BAD_REQUEST, "허용 인원은 1명 이상이어야 합니다.");
    }

    ShareTicket shareTicket = ShareTicket.builder()
        .linkToken(token)
        .totalAllowed(dto.getTotalAllowed())
        .expired(false)
        .submittedCount(1)
        .reservation(reservation)
        .expiredAt(reservation.getSchedule().getDate().minusDays(1)
            .atStartOfDay())
        .build();

    shareTicketRepository.save(shareTicket);
    return token;
  }

  public ShareTicketInfoResponseDto getFormInfo(String token) {
    ShareTicket shareTicket = validateAndUseToken(token);

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
