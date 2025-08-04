package com.fairing.fairplay.booth.repository;

import com.fairing.fairplay.booth.entity.BoothPaymentStatusCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoothPaymentStatusCodeRepository extends JpaRepository<BoothPaymentStatusCode, Integer> {
    // 결제 상태 코드 조회 (PENDING, PAID 등)
    Optional<BoothPaymentStatusCode> findByCode(String code);
}