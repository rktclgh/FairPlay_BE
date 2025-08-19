package com.fairing.fairplay.core.email.service;

import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import com.fairing.fairplay.admin.repository.EmailTemplatesRepository;
import com.fairing.fairplay.core.util.EmailSender;

// QR 티켓 링크 발급 완료 이메일
@Service
public class SuccessQrTicketEmailService extends AbstractEmailService {

  public SuccessQrTicketEmailService(EmailSender emailSender,
      EmailTemplatesRepository emailTemplatesRepository) {
    super(emailSender, emailTemplatesRepository);
  }

  @Override
  protected EmailContent createEmailContent(Object... params) {
    String name = (String) params[0]; // 참석자 이름

    String escapedName = HtmlUtils.htmlEscape(name);

    // 템플릿 파일(success-qr-ticket.html)에 %s로 인증코드 바인딩
    String html = String.format(loadTemplate("success-qr-ticket.html"), escapedName);
    return new EmailContent("[FairPlay] QR 티켓 발급 완료", html, "logo", "etc/logo.png");
  }
}
