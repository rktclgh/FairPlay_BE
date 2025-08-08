package com.fairing.fairplay.payment.repository;

import com.fairing.fairplay.payment.entity.PaymentStatusCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentStatusCodeRepository extends JpaRepository<PaymentStatusCode, Integer> {
}
