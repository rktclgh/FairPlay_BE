package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.qr.entity.QrCheckLog;
import com.fairing.fairplay.qr.entity.QrCheckStatusCode;
import com.fairing.fairplay.qr.entity.QrTicket;
import com.fairing.fairplay.qr.repository.QrActionCodeRepository;
import com.fairing.fairplay.qr.repository.QrCheckLogRepository;
import com.fairing.fairplay.qr.repository.QrCheckStatusCodeRepository;
import com.fairing.fairplay.qr.repository.QrLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

// QR과 관련된 로그 서비스
@Service
@RequiredArgsConstructor
public class QrLogService {

  private final QrLogRepository qrLogRepository;
  private final QrCheckLogRepository qrCheckLogRepository;
  private final QrActionCodeRepository qrActionCodeRepository;
  private final QrCheckStatusCodeRepository qrCheckStatusCodeRepository;

  // qr 체크인/체크아웃 로그 저장
  public QrCheckLog save(QrTicket qrTicket, String checkStatusCode){
    QrCheckLog qrCheckLog = QrCheckLog.builder()
        .qrTicket(qrTicket)
        .checkStatusCode(findByCode(checkStatusCode))
        .build();
    return qrCheckLogRepository.save(qrCheckLog);
  }

  private QrCheckStatusCode findByCode(String qrCheckStatusCode) {
    return qrCheckStatusCodeRepository.findByCode(qrCheckStatusCode).orElseThrow(() -> new CustomException(
        HttpStatus.INTERNAL_SERVER_ERROR,"QR 체크인 상태 코드가 올바르지 않습니다."));
  }
}
