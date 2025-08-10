package com.fairing.fairplay.payment.repository;

import com.fairing.fairplay.payment.entity.RefundStatusCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefundStatusCodeRepository extends JpaRepository<RefundStatusCode, Integer> {
    
    Optional<RefundStatusCode> findByCode(String code);
}