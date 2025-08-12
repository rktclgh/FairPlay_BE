package com.fairing.fairplay.ticket.repository;

import com.fairing.fairplay.ticket.entity.TicketAudienceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketAudienceTypeRepository extends JpaRepository<TicketAudienceType, Integer> {

    Optional<TicketAudienceType> findByCode(String code);
}
