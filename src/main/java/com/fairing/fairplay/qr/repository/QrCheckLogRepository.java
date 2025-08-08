package com.fairing.fairplay.qr.repository;

import com.fairing.fairplay.qr.entity.QrCheckLog;
import com.fairing.fairplay.qr.entity.QrTicket;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface QrCheckLogRepository extends CrudRepository<QrCheckLog, Long> {

  Optional<QrCheckLog> findTop1ByQrTicketAndCheckStatusCode_CodeOrderByCreatedAtDesc(
      QrTicket qrTicket, String checkStatusCode);

  QrCheckLog existsQrTicketAndCheckStatusCode_Code(String checkStatusCodeCode);
}
