package com.fairing.fairplay.shareticket.service;

import com.fairing.fairplay.reservation.repository.ReservationRepository;
import com.fairing.fairplay.shareticket.entity.ShareTicket;
import com.fairing.fairplay.shareticket.repository.ShareTicketRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShareTicketBatchService {

  private final ShareTicketRepository shareTicketRepository;

  // 만료 날짜가 오늘이고 아직 만료처리되지 않은 공유 폼 링크 조회
  // 당일 예약이거나 행사 전날 예약했을 경우엔 제외
  public List<ShareTicket> fetchExpiredBatch(int page, int size) {
    Pageable pageable = PageRequest.of(page, size);

    LocalDate now = LocalDate.now(); // 2025-08-10
    LocalDateTime startDate = now.atStartOfDay();
    LocalDateTime endDate = now.plusDays(1).atStartOfDay(); // 11
    log.info("fetchExpiredBatch startDate: {}, endDate: {}", startDate, endDate);

    return shareTicketRepository.findAllExpiredExceptTodayReservations(endDate, now,  pageable);
  }

  // 공유 폼 링크 만료 -> 스케줄러 자동 실행
  @Transactional
  public void expiredToken(List<ShareTicket> shareTickets) {
    // 폼링크 자동 만료
    shareTickets.forEach(shareTicket -> {
      shareTicket.setExpired(true);
    });

    log.info("expiredToken: {}", shareTickets.size());
    shareTicketRepository.saveAll(shareTickets);
    shareTicketRepository.flush();
  }

  // 만료된 정보 삭제
  public void deleteShareTicket(ShareTicket shareTicket) {
    // 취소된 티켓일 경우
    if (shareTicket.getReservation().isCanceled()) {
      shareTicket.setExpired(true);
    }
  }
}
