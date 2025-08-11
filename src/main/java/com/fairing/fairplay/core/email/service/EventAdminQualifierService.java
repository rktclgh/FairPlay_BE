package com.fairing.fairplay.core.email.service;

import org.springframework.stereotype.Service;

import com.fairing.fairplay.core.util.EmailSender;

@Service
public class EventAdminQualifierService extends AbstractEmailService {

    public EventAdminQualifierService(EmailSender emailSender) {
        super(emailSender);
    }

    @Override
    protected EmailContent createEmailContent(Object... params) {
        String eventAdminName = (String) params[0]; // 행사관리자 이름
        String password = (String) params[1]; // 임시 비밀번호
        String html = String.format(loadTemplate("qualifier.html"), password);
        return new EmailContent("[FairPlay] 이벤트 관리자 계정 전달", html, "logo", "etc/logo.png");
    }

    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        java.security.SecureRandom random = new java.security.SecureRandom();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
