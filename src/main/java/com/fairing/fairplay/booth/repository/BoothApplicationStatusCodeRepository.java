package com.fairing.fairplay.booth.repository;

import com.fairing.fairplay.booth.entity.BoothApplicationStatusCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoothApplicationStatusCodeRepository extends JpaRepository<BoothApplicationStatusCode, Integer> {
    // 신청 상태 코드 조회 (PENDING, APPROVED 등)
    Optional<BoothApplicationStatusCode> findByCode(String code);
}
