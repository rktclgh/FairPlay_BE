package com.fairing.fairplay.core.email.service;

import com.fairing.fairplay.admin.repository.EmailTemplatesRepository;
import com.fairing.fairplay.core.util.EmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import java.text.DecimalFormat;

@Service
@Slf4j
public class BannerEmailService extends AbstractEmailService {

    public BannerEmailService(EmailSender emailSender,
            EmailTemplatesRepository emailTemplatesRepository) {
        super(emailSender, emailTemplatesRepository);
    }

    public void sendRejectionEmail(String to, String title, String reason) {
        send(to, "rejection", title, reason);
    }

    public void sendApprovalWithPaymentEmail(String to, String title, String bannerType, 
            Integer totalAmount, Long applicationId, String applicantName) {
        send(to, "approval", title, bannerType, totalAmount, applicationId, applicantName);
    }

    @Override
    protected EmailContent createEmailContent(Object... params) {
        String type = (String) params[0];
        String subject;
        String htmlContent;

        if ("rejection".equals(type)) {
            String title = HtmlUtils.htmlEscape((String) params[1]);
            String reason = HtmlUtils.htmlEscape((String) params[2]);
            subject = "[FairPlay] 배너 광고 신청이 반려되었습니다.";
            htmlContent = buildHtmlContent("banner-rejection.html", "logo", title, reason);
        } else if ("approval".equals(type)) {
            String title = HtmlUtils.htmlEscape((String) params[1]);
            String bannerType = HtmlUtils.htmlEscape((String) params[2]);
            Integer totalAmount = (Integer) params[3];
            Long applicationId = (Long) params[4];
            String applicantName = HtmlUtils.htmlEscape((String) params[5]);
            
            // 결제 링크 생성 (프론트엔드 배너 결제 페이지)
            String paymentUrl = String.format("https://fair-play.ink/banner/payment?applicationId=%d", applicationId);
            
            subject = "[FairPlay] 배너 광고 신청이 승인되었습니다! 결제를 진행해주세요.";
            htmlContent = buildHtmlContent("banner-approval.html", "logo", title, bannerType,
                    formatPrice(totalAmount), paymentUrl, applicantName);
        } else {
            throw new IllegalArgumentException("Unsupported email type: " + type);
        }

        return new EmailContent(subject, htmlContent, "logo", "/etc/logo.png");
    }

    private String buildHtmlContent(String templateFileName, Object... args) {
        try {
            String template = loadTemplate(templateFileName);
            return String.format(template, args);
        } catch (Exception e) {
            log.error("HTML 템플릿 생성 실패 - template: {}, args: {}, error: {}", 
                templateFileName, java.util.Arrays.toString(args), e.getMessage());
            // 폴백: 기본 템플릿 사용
            return createFallbackHtmlContent(templateFileName, args);
        }
    }
    
    private String createFallbackHtmlContent(String templateFileName, Object... args) {
        StringBuilder fallback = new StringBuilder();
        fallback.append("<html><body>");
        fallback.append("<h1>FairPlay</h1>");
        if (templateFileName.contains("approval")) {
            fallback.append("<p>배너 광고 신청이 승인되었습니다.</p>");
            if (args.length > 0) fallback.append("<p>제목: ").append(args[1]).append("</p>");
            if (args.length > 4) fallback.append("<p>결제 링크: <a href='").append(args[4]).append("'>결제하기</a></p>");
        } else if (templateFileName.contains("rejection")) {
            fallback.append("<p>배너 광고 신청이 반려되었습니다.</p>");
            if (args.length > 1) fallback.append("<p>제목: ").append(args[1]).append("</p>");
            if (args.length > 2) fallback.append("<p>사유: ").append(args[2]).append("</p>");
        }
        fallback.append("</body></html>");
        return fallback.toString();
    }

    /**
     * 가격 포맷팅 (천 단위 구분자 추가)
     */
    private String formatPrice(Integer amount) {
        if (amount == null) return "0";
        try {
            DecimalFormat formatter = new DecimalFormat("#,###");
            return formatter.format(amount.longValue()) + "원";
        } catch (Exception e) {
            log.error("가격 포맷팅 실패 - amount: {}, error: {}", amount, e.getMessage());
            return amount.toString() + "원";
        }
    }
}