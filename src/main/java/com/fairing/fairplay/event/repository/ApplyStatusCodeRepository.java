package com.fairing.fairplay.event.repository;

import com.fairing.fairplay.event.entity.ApplyStatusCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplyStatusCodeRepository extends JpaRepository<ApplyStatusCode, Integer> {
    
    Optional<ApplyStatusCode> findByCode(String code);
}