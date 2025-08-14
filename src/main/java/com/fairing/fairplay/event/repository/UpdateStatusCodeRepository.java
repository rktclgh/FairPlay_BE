package com.fairing.fairplay.event.repository;

import com.fairing.fairplay.event.entity.UpdateStatusCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UpdateStatusCodeRepository extends JpaRepository<UpdateStatusCode, Long> {
    
    Optional<UpdateStatusCode> findByCode(String code);
}