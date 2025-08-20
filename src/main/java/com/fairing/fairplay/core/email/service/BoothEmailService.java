package com.fairing.fairplay.core.email.service;

import com.fairing.fairplay.admin.repository.EmailTemplatesRepository;
import com.fairing.fairplay.core.util.EmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
@Slf4j
public class BoothEmailService extends AbstractEmailService {

    public BoothEmailService(EmailSender emailSender,
            EmailTemplatesRepository emailTemplatesRepository) {
        super(emailSender, emailTemplatesRepository);
    }

    public void sendRejectionEmail(String to, String eventTitle, String boothTitle, String reason) {
        send(to, "rejection", eventTitle, boothTitle, reason);
    }

    public void sendApprovalEmail(String to, String eventTitle, String boothTitle, String boothEmail,
            String tempPassword, String boothTypeName, String boothSize, Integer price, Long applicationId) {
        send(to, "approval", eventTitle, boothTitle, boothEmail, tempPassword, boothTypeName, boothSize, price,
                applicationId);
    }

    public void sendCancelConfirmationEmail(String to, String eventTitle, String boothTitle, String cancelReason) {
        send(to, "cancel", eventTitle, boothTitle, cancelReason);
    }

    @Override
    protected EmailContent createEmailContent(Object... params) {
        String type = (String) params[0];
        String eventTitle = HtmlUtils.htmlEscape((String) params[1]);
        String boothTitle = HtmlUtils.htmlEscape((String) params[2]);
        String subject;
        String htmlContent;

        if ("rejection".equals(type)) {
            String reason = HtmlUtils.htmlEscape((String) params[3]);
            subject = "[FairPlay] 부스 등록 신청이 반려되었습니다.";
            htmlContent = buildHtmlContent("booth-rejection.html", "logo", eventTitle, boothTitle, reason);
        } else if ("approval".equals(type)) {
            String boothEmail = HtmlUtils.htmlEscape((String) params[3]);
            String tempPassword = HtmlUtils.htmlEscape((String) params[4]);
            String boothTypeName = HtmlUtils.htmlEscape((String) params[5]);
            String boothSize = HtmlUtils.htmlEscape((String) params[6]);
            Integer price = (Integer) params[7];
            Long applicationId = (Long) params[8];
            subject = "[FairPlay] 부스 등록 신청이 승인되었습니다! 계정 안내 및 결제 요청";
            // 참고: applicationId가 마지막에 두 번 전달되고 있습니다. 템플릿 확인이 필요합니다.
            htmlContent = buildHtmlContent("booth-approval.html", "logo", eventTitle, boothTitle, boothEmail,
                    tempPassword, boothTypeName, boothSize, price, applicationId, applicationId);
        } else if ("cancel".equals(type)) {
            String cancelReason = HtmlUtils.htmlEscape((String) params[3]);
            subject = "[FairPlay] 부스 취소 확인";
            htmlContent = buildHtmlContent("booth-cancel.html", "logo", eventTitle, boothTitle, cancelReason);
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
