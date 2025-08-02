package com.fairing.fairplay.shareticket.repository;

import com.fairing.fairplay.shareticket.entity.ShareTicket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShareTicketRepository extends JpaRepository<ShareTicket, Long> {

  Optional<ShareTicket> findByLinkToken(String linkToken);
  List<ShareTicket> findAllByExpiredAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}
