package com.fairing.fairplay.core.email.service;

import org.springframework.stereotype.Service;

@Service
public class TemporaryPasswordEmailService extends AbstractEmailService {
    public TemporaryPasswordEmailService(com.fairing.fairplay.core.util.EmailSender emailSender) {
        super(emailSender);
    }

    @Override
    protected EmailContent createEmailContent(Object... params) {
        String tempPassword = (String) params[0];
        // 템플릿 파일(temporary-password.html)에 %s로 임시비밀번호 바인딩
        String html = String.format(loadTemplate("temporary-password.html"), tempPassword);
        return new EmailContent("[FairPlay] 임시 비밀번호 안내", html, "logo", "etc/logo.png");
    }
}
