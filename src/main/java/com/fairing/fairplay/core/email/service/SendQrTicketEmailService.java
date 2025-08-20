package com.fairing.fairplay.core.email.service;

import com.fairing.fairplay.admin.repository.EmailTemplatesRepository;
import com.fairing.fairplay.core.util.EmailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

// qr티켓 이메일 내용 생성 서비스
@Service
public class SendQrTicketEmailService extends AbstractEmailService {

  public SendQrTicketEmailService(EmailSender emailSender,
      EmailTemplatesRepository emailTemplatesRepository) {
    super(emailSender, emailTemplatesRepository);
  }

  @Override
  protected EmailContent createEmailContent(Object... params) {
    String name = (String) params[0]; // 참석자 이름
    String qrUrl = (String) params[1]; // 참석자 qr 티켓 링크
    String eventName = (String) params[2]; // 행사명
    String eventDate = (String) params[3]; // 행사 일자
    String viewingDate = (String) params[4]; // 관람 일자

    String escapedName = HtmlUtils.htmlEscape(name);
    String escapedEventName = HtmlUtils.htmlEscape(eventName);
    String escapedEventDate = HtmlUtils.htmlEscape(eventDate);
    String escapedViewingDate = HtmlUtils.htmlEscape(viewingDate);

    // 템플릿 파일(qr-ticket.html)에 %s로 인증코드 바인딩
    String html = String.format(loadTemplate("qr-ticket.html"), escapedName, escapedEventName, escapedEventDate,
        escapedViewingDate, qrUrl);
    return new EmailContent("[" + eventName + "] " + "QR 티켓 발송", html, "logo", "etc/logo.png");
  }
}
