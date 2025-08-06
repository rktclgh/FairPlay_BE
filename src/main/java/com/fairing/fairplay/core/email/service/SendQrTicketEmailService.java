package com.fairing.fairplay.core.email.service;

import com.fairing.fairplay.core.util.EmailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

// qr티켓 이메일 내용 생성 서비스
@Service
public class SendQrTicketEmailService extends AbstractEmailService {

  public SendQrTicketEmailService(EmailSender emailSender) {
    super(emailSender);
  }

  @Override
  protected EmailContent createEmailContent(Object... params) {
    String name = (String) params[0]; // 참석자 이름
    String qrUrl = (String) params[1]; // 참석자 qr 티켓 링크

    String escapedName = HtmlUtils.htmlEscape(name);

    // 템플릿 파일(qr-ticket.html)에 %s로 인증코드 바인딩
    String html = String.format(loadTemplate("qr-ticket.html"), escapedName, qrUrl);
    return new EmailContent("[FairPlay] QR 티켓 발송", html, "logo", "etc/logo.png");
  }
}
