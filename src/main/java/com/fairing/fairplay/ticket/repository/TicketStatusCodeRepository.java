package com.fairing.fairplay.ticket.repository;

import com.fairing.fairplay.ticket.entity.TicketStatusCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketStatusCodeRepository extends JpaRepository<TicketStatusCode, Integer> {

    Optional<TicketStatusCode> findByCode(String statusCode);
}
