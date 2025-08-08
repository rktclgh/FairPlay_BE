package com.fairing.fairplay.qr.repository;

import com.fairing.fairplay.qr.entity.QrLog;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface QrLogRepository extends CrudRepository<QrLog, Long> {
  Optional<QrLog> findQrActionCode_Code(String qrCode);

}
