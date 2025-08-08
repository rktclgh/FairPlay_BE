package com.fairing.fairplay.qr.repository;

import com.fairing.fairplay.qr.entity.QrLog;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QrLogRepository extends JpaRepository<QrLog, Long> {
  Optional<QrLog> findByActionCode_Code(String qrCode);

}
