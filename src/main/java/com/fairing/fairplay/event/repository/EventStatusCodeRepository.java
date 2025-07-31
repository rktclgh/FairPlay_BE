package com.fairing.fairplay.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fairing.fairplay.event.entity.EventStatusCode;

@Repository
public interface EventStatusCodeRepository extends JpaRepository<EventStatusCode, Integer> {
    
}
