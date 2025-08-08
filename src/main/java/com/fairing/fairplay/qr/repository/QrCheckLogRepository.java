package com.fairing.fairplay.qr.repository;

import com.fairing.fairplay.qr.entity.QrCheckLog;
import com.fairing.fairplay.qr.entity.QrTicket;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QrCheckLogRepository extends JpaRepository<QrCheckLog, Long> {

  Optional<QrCheckLog> findTop1ByQrTicketAndCheckStatusCode_CodeOrderByCreatedAtDesc(
      QrTicket qrTicket, String checkStatusCode);

  boolean existsByQrTicketAndCheckStatusCode_Code(QrTicket qrTicket, String checkStatusCodeCode);
}
