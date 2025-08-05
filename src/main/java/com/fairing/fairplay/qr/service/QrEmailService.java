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

  public void sendQrEmail(String qrUrl, String email, String name) {
    emailServiceFactory.getService(EmailType.QR_TICKET)
        .send(email, name, qrUrl);
  }
}
