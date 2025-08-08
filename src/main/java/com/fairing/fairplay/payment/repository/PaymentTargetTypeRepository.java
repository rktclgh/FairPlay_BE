package com.fairing.fairplay.payment.repository;

import com.fairing.fairplay.payment.entity.PaymentTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentTargetTypeRepository extends JpaRepository<PaymentTargetType, Long> {
    
    Optional<PaymentTargetType> findByPaymentTargetCode(String paymentTargetCode);
}