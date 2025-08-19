package com.fairing.fairplay.core.email.service;

import org.springframework.stereotype.Service;

import com.fairing.fairplay.admin.repository.EmailTemplatesRepository;

@Service
public class VerificationEmailService extends AbstractEmailService {
    public VerificationEmailService(com.fairing.fairplay.core.util.EmailSender emailSender,
            EmailTemplatesRepository emailTemplatesRepository) {
        super(emailSender, emailTemplatesRepository);
    }

    @Override
    protected EmailContent createEmailContent(Object... params) {
        String code = (String) params[0];
        // 템플릿 파일(verification.html)에 %s로 인증코드 바인딩
        String html = String.format(loadTemplate("verification.html"), code);
        return new EmailContent("[FairPlay] 이메일 인증코드", html, "logo", "etc/logo.png");
    }
}
