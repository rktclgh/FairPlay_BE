package com.fairing.fairplay.payment.repository;

import com.fairing.fairplay.payment.entity.PaymentStatusCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentStatusCodeRepository extends JpaRepository<PaymentStatusCode, Integer> {

    PaymentStatusCode getReferenceByCode(String pending);
    
    Optional<PaymentStatusCode> findByCode(String code);
}
