package com.fairing.fairplay.reservation.repository;

import com.fairing.fairplay.reservation.entity.ReservationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationLogRepository extends JpaRepository<ReservationLog, Long> {
}
