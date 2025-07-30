package com.fairing.fairplay.shareticket.service;

import com.fairing.fairplay.shareticket.dto.ShareTicketSaveRequestDto;
import com.fairing.fairplay.shareticket.entity.ShareTicket;
import com.fairing.fairplay.shareticket.repository.ShareTicketRepository;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShareTicketService {

  private final ShareTicketRepository shareTicketRepository;

  // 공유폼 token 유효성 검사
  @Transactional
  public ShareTicket validateAndUseToken(String token) {
    ShareTicket shareTicket = shareTicketRepository.findByLinkToken(token)
        .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 링크입니다."));

    if (shareTicket.getExpired()) {
      throw new IllegalStateException("링크가 만료되었습니다.");
    }

    if (shareTicket.isFull()) {
      throw new IllegalStateException("입력 가능한 인원을 초과했습니다.");
    }

    shareTicket.increaseSubmittedCount();

    // 만약 제출 수가 허용 수에 도달하면 링크 만료 처리
    if (shareTicket.getSubmittedCount() >= shareTicket.getTotalAllowed()) {
      shareTicket.setExpired(true);
    }

    return shareTicket;
  }

  // 공유 폼 링크 생성
  @Transactional
  public String generateToken(ShareTicketSaveRequestDto dto) {

    UUID uuid = UUID.randomUUID();
    byte[] bytes = new byte[16];
    ByteBuffer.wrap(bytes)
        .putLong(uuid.getMostSignificantBits())
        .putLong(uuid.getLeastSignificantBits());
    String token = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(bytes); //5B3Ej0AdRMqrqY7xV6k9tw 형태

    ShareTicket shareTicket = ShareTicket.builder()
        .linkToken(token)
        .totalAllowed(dto.getTotalAllowed())
        .expired(false)
        .submittedCount(0)
        .reservationId(dto.getReservationId())
        .expiredAt(dto.getExpiredAt())
        .build();

    shareTicketRepository.save(shareTicket);
    return token;
  }
}
