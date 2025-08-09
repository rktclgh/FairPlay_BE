package com.fairing.fairplay.qr.repository;

import com.fairing.fairplay.qr.entity.QrLog;
import com.fairing.fairplay.qr.entity.QrTicket;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QrLogRepository extends JpaRepository<QrLog, Long> {
  Optional<QrLog> findByActionCode_Code(String qrActionCode);

  boolean existsByQrTicketAndActionCode_Code(QrTicket qrTicket, String qrActionCode);
}
