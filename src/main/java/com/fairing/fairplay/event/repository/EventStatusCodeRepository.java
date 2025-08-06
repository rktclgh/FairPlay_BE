package com.fairing.fairplay.event.repository;

import com.fairing.fairplay.event.entity.EventStatusCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventStatusCodeRepository extends JpaRepository<EventStatusCode, Integer> {
    Optional<EventStatusCode> findByCode(String code);
}
