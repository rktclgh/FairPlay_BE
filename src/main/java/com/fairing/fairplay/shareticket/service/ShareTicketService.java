package com.fairing.fairplay.shareticket.service;

import com.fairing.fairplay.common.exception.LinkExpiredException;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import com.fairing.fairplay.shareticket.dto.ShareTicketSaveRequestDto;
import com.fairing.fairplay.shareticket.entity.ShareTicket;
import com.fairing.fairplay.shareticket.repository.ShareTicketRepository;
import jakarta.persistence.EntityNotFoundException;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShareTicketService {

  private final ShareTicketRepository shareTicketRepository;
  private final ReservationRepository reservationRepository;

  // 공유 폼 링크 생성 -> 예약 성공 시 예약 서비스 단계에서 사용
  @Transactional
  public String generateToken(ShareTicketSaveRequestDto dto) {
    if (dto == null) {
      throw new IllegalArgumentException("유효하지 않은 요청입니다.");
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

    Reservation reservation = reservationRepository.findById(dto.getReservationId())
        .orElseThrow(() -> new EntityNotFoundException("예약을 찾을 수 없습니다."));

    ShareTicket shareTicket = ShareTicket.builder()
        .linkToken(token)
        .totalAllowed(dto.getTotalAllowed()-1) //대표자 제출 O
        .expired(false)
        .submittedCount(1) // 대표자 제출
        .reservation(reservation)
        .expiredAt(dto.getExpiredAt())
        .build();

    shareTicketRepository.save(shareTicket);
    return token;
  }

  // 공유폼 token 유효성 검사 -> attendeeService에서 호출해 사용
  @Transactional
  public ShareTicket validateAndUseToken(String token) {
    ShareTicket shareTicket = shareTicketRepository.findByLinkToken(token)
        .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 링크입니다."));

    // 만료된 링크 (행사시작 1일전)
    if (shareTicket.getExpired()) {
      throw new LinkExpiredException("링크가 만료되었습니다.", "/link-expired");
    }

    // 티켓 구매 수 == 폼 제출 횟수
    if (shareTicket.isFull()) {
      throw new LinkExpiredException("참가자 인원이 모두 등록되었습니다.", "/link-closed");
    }
    return shareTicket;
  }

  // 폼 링크 제출 횟수 및 만료 여부 업데이트
  @Transactional
  public void updateShareTicket(ShareTicket shareTicket) {
    shareTicket.increaseSubmittedCount();

    // 만약 제출 수가 허용 수에 도달하면 링크 만료 처리
    if (shareTicket.getSubmittedCount() >= shareTicket.getTotalAllowed()) {
      shareTicket.setExpired(true);
    }
  }

  // 만료 날짜가 오늘이고 아직 만료처리되지 않은 공유 폼 링크 조회
  public List<ShareTicket> fetchExpiredBatch(int page, int size) {
    Pageable pageable = PageRequest.of(page, size);

    LocalDate now = LocalDate.now();
    LocalDateTime startDate = now.atStartOfDay();
    LocalDateTime endDate = now.atTime(23, 59, 59);

    return shareTicketRepository.findAllByExpiredAtBetweenAndExpiredFalse(startDate, endDate,
        pageable);
  }

  // 공유 폼 링크 만료 -> 스케줄러 자동 실행
  @Transactional
  public void expiredToken(List<ShareTicket> shareTickets) {
    // 폼링크 자동 만료
    shareTickets.forEach(shareTicket -> {
      shareTicket.setExpired(true);
      shareTicket.setExpiredAt(LocalDateTime.now());
    });
    shareTicketRepository.saveAll(shareTickets);
  }
}
