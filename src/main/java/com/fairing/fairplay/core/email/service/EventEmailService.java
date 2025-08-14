package com.fairing.fairplay.core.email.service;

import com.fairing.fairplay.core.util.EmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EventEmailService extends AbstractEmailService {

    public EventEmailService(EmailSender emailSender) {
        super(emailSender);
    }

    public void sendRejectionEmail(String to, String eventTitle, String reason) {
        send(to, "rejection", eventTitle, reason);
    }

    public void sendApprovalEmail(String to, String eventTitle, String username, String tempPassword) {
        send(to, "approval", eventTitle, username, tempPassword);
    }

    @Override
    protected EmailContent createEmailContent(Object... params) {
        String type = (String) params[0];
        String eventTitle = (String) params[1];
        String subject;
        String htmlContent;

        if ("rejection".equals(type)) {
            String reason = (String) params[2];
            subject = "[FairPlay] 행사 등록 신청이 반려되었습니다.";
            htmlContent = buildHtmlContent("event-rejection.html", "logo", eventTitle, reason);
        } else if ("approval".equals(type)) {
            String username = (String) params[2];
            String tempPassword = (String) params[3];
            subject = "[FairPlay] 행사 등록 신청이 승인되었습니다! 계정 안내";
            htmlContent = buildHtmlContent("event-approval.html", "logo", eventTitle, username, tempPassword);
        } else {
            throw new IllegalArgumentException("Unsupported email type: " + type);
        }

        return new EmailContent(subject, htmlContent, "logo", "/etc/logo.png");
    }

    private String buildHtmlContent(String templateFileName, Object... args) {
        String template = loadTemplate(templateFileName);
        return String.format(template, args);
    }
}
