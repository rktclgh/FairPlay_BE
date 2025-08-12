package com.fairing.fairplay.ticket.repository;

import com.fairing.fairplay.ticket.entity.TicketSeatType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketSeatTypeRepository extends JpaRepository<TicketSeatType, Integer> {

    Optional<TicketSeatType> findByCode(String seatTypeCode);
}
