package com.fairing.fairplay.reservation.repository;

import com.fairing.fairplay.reservation.entity.ReservationStatusCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReservationStatusCodeRepository extends JpaRepository<ReservationStatusCode, Integer> {
    Optional<ReservationStatusCode> findByCode(String code);
}