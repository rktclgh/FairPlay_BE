package com.fairing.fairplay.payment.repository;

import com.fairing.fairplay.payment.entity.PaymentTypeCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentTypeCodeRepository extends JpaRepository<PaymentTypeCode, Integer> {

    PaymentTypeCode getReferenceByCode(String targetType);
    Optional<PaymentTypeCode> findByCode(String code);
}
