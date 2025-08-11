package com.fairing.fairplay.qr.repository;

import com.fairing.fairplay.qr.entity.QrActionCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QrActionCodeRepository extends JpaRepository<QrActionCode, Integer> {
  Optional<QrActionCode> findByCode(String qrActionCode);

}
