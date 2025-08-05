package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.core.email.entity.EmailServiceFactory;
import com.fairing.fairplay.core.email.entity.EmailServiceFactory.EmailType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// QR 이메일 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class QrEmailService {

  private final EmailServiceFactory emailServiceFactory;

  // 행사 1일 전 QR 티켓 이메일 전송
  public void sendQrEmail(String qrUrl, String email, String name) {
    emailServiceFactory.getService(EmailType.SEND_QR_TICKET)
        .send(email, name, qrUrl);
  }

  // QR 티켓 강제 재발급 완료 이메일
  public void successSendQrEmail(String email, String name) {
    emailServiceFactory.getService(EmailType.SUCCESS_QR_TICKET)
        .send(email, name);
  }

  // QR 티켓 강제 재발급해 이메일로 전송
  public void reissueQrEmail(String qrUrl, String email, String name) {
    emailServiceFactory.getService(EmailType.SEND_QR_TICKET)
        .send(email, name, qrUrl);
  }
}
