package com.fairing.fairplay.settlement.repository;

import com.fairing.fairplay.settlement.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement,Long> {
    Optional<Settlement> findByEvent_EventId(Long eventId);
    boolean existsByEvent_EventId(Long eventId);
}
