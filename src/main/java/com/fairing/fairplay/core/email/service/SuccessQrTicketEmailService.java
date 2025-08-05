package com.fairing.fairplay.core.email.service;

import com.fairing.fairplay.core.util.EmailSender;
import org.springframework.stereotype.Service;

// QR 티켓 링크 발급 완료 이메일
@Service
public class SuccessQrTicketEmailService extends AbstractEmailService{
  public SuccessQrTicketEmailService(EmailSender emailSender) {
    super(emailSender);
  }

  @Override
  protected EmailContent createEmailContent(Object... params) {
    String name = (String) params[0]; // 참석자 이름
    // 템플릿 파일(verification.html)에 %s로 인증코드 바인딩
    String html = String.format(loadTemplate("success-qr-ticket.html"), name);
    return new EmailContent("[FairPlay] QR 티켓 발급 완료", html, "logo", "etc/logo.png");
  }
}
