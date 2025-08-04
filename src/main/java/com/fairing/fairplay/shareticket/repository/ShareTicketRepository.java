package com.fairing.fairplay.shareticket.repository;

import com.fairing.fairplay.shareticket.entity.ShareTicket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShareTicketRepository extends JpaRepository<ShareTicket, Long> {

  Optional<ShareTicket> findByLinkToken(String linkToken);

  List<ShareTicket> findAllByExpiredAtBetweenAndExpiredFalse(LocalDateTime startDate, LocalDateTime endDate,
      Pageable pageable);

  boolean existsByLinkToken(String token);
  boolean existsByReservation_ReservationId(Long reservationId);
}
