package com.fairing.fairplay.qr.repository;

import com.fairing.fairplay.qr.entity.QrCheckStatusCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QrCheckStatusCodeRepository extends JpaRepository<QrCheckStatusCode, Integer> {
  Optional<QrCheckStatusCode> findByCode(String qrCheckStatusCode);
}
