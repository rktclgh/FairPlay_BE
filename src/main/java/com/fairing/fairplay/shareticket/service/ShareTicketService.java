package com.fairing.fairplay.shareticket.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.common.exception.LinkExpiredException;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventRepository;


import com.fairing.fairplay.payment.repository.PaymentRepository;
import com.fairing.fairplay.qr.dto.QrTicketRequestDto;
import com.fairing.fairplay.qr.util.CodeGenerator;
import com.fairing.fairplay.qr.util.CodeValidator;
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
import org.springframework.web.bind.annotation.RequestParam;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShareTicketService {

  private final ShareTicketRepository shareTicketRepository;
  private final ReservationRepository reservationRepository;
  private final PaymentRepository paymentRepository;

  private static final String RESERVATION = "RESERVATION";
  private static final String COMPLETED = "COMPLETED";
  private final CodeValidator codeValidator;
  private final EventRepository eventRepository;

  // 공유 폼 링크 생성 -> 예약 성공 시 예약 서비스 단계에서 사용
  @Transactional
  public String generateToken(ShareTicketSaveRequestDto dto) {
    if (dto == null) {
      throw new IllegalArgumentException("유효하지 않은 요청입니다.");
    }

    // 예약 유무 조회
    Reservation reservation = reservationRepository.findById(dto.getReservationId())
        .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다."));

    // 결제 내역 조회

    boolean hasCompleted = paymentRepository
        .existsByTargetIdAndPaymentTargetType_PaymentTargetCodeAndPaymentStatusCode_Code(
            reservation.getReservationId(), RESERVATION, COMPLETED);
    if (!hasCompleted) {
      throw new CustomException(HttpStatus.BAD_REQUEST, "결제가 정상적으로 처리되지 않았습니다.");
    }

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

    if (dto.getTotalAllowed() == null || dto.getTotalAllowed() <= 0) {
      throw new CustomException(HttpStatus.BAD_REQUEST, "허용 인원은 1명 이상이어야 합니다.");
    }

    ShareTicket shareTicket = ShareTicket.builder()
        .linkToken(token) // 폼 링크 토큰
        .totalAllowed(dto.getTotalAllowed()) //대표자 제출 O
        .expired(false) // 만료 여부
        .submittedCount(1) // 대표자 제출
        .reservation(reservation) // 예약 연결
        .expiredAt(reservation.getSchedule().getDate().minusDays(1)
            .atStartOfDay()) // 폼 만료 기한 (행사 시작일 하루 전)
        .build();

    shareTicketRepository.save(shareTicket);
    return token;
  }

  public ShareTicketInfoResponseDto getFormInfo(@RequestParam String token) {
    ShareTicket shareTicket = shareTicketRepository.findByLinkToken(token).orElseThrow(
        () -> new CustomException(HttpStatus.BAD_REQUEST,"잘못된 폼 링크입니다.")
    );

    QrTicketRequestDto dto = codeValidator.decodeToDto(token);
    Event event = eventRepository.findById(dto.getEventId()).orElseThrow(
        () -> new CustomException(HttpStatus.NOT_FOUND, "적절한 행사가 조회되지 않습니다.")
    );

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

    // 만료된 링크 (행사시작 1일전)
    if (shareTicket.getExpired()) {
      throw new LinkExpiredException("해당 링크는 더 이상 유효하지 않습니다.", "/link-expired");
    }

    // 티켓 구매 수 == 폼 제출 횟수
    if (shareTicket.isFull()) {
      throw new LinkExpiredException("참가자 등록이 이미 마감되었습니다.", "/link-closed");
    }
    return shareTicket;
  }

  // 폼 링크 제출 횟수 및 만료 여부 업데이트
  public void updateShareTicket(ShareTicket shareTicket) {
    shareTicket.increaseSubmittedCount();

    // 만약 제출 수가 허용 수에 도달하면 링크 만료 처리
    if (shareTicket.getSubmittedCount() >= shareTicket.getTotalAllowed()) {
      shareTicket.setExpired(true);
    }
  }
}
