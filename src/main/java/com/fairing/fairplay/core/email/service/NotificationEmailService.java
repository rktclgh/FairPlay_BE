package com.fairing.fairplay.core.email.service;

import com.fairing.fairplay.core.email.service.AbstractEmailService;
import com.fairing.fairplay.core.util.EmailSender;
import com.fairing.fairplay.notification.entity.Notification;
import com.fairing.fairplay.notification.entity.NotificationLog;
import com.fairing.fairplay.notification.entity.NotificationMethodCode;
import com.fairing.fairplay.notification.repository.NotificationLogRepository;
import com.fairing.fairplay.notification.repository.NotificationMethodCodeRepository;
import com.fairing.fairplay.notification.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class NotificationEmailService extends AbstractEmailService {

    private final NotificationRepository notificationRepository;
    private final NotificationMethodCodeRepository methodCodeRepository;
    private final NotificationLogRepository notificationLogRepository;

    public NotificationEmailService(
            EmailSender emailSender,
            NotificationRepository notificationRepository,
            NotificationMethodCodeRepository methodCodeRepository,
            NotificationLogRepository notificationLogRepository
    ) {
        super(emailSender);
        this.notificationRepository = notificationRepository;
        this.methodCodeRepository = methodCodeRepository;
        this.notificationLogRepository = notificationLogRepository;
    }

    /**
     * 알림(일반용) 이메일 전송
     *
     * @param notificationId  알림 엔티티 PK
     * @param to              수신자 이메일
     * @param title           알림 제목 (이메일 subject 및 템플릿에 노출)
     * @param message         알림 본문 (이메일 템플릿에 노출)
     * @param url             상세 보기 버튼 등 (null 가능)
     */
    @Transactional
    public void sendEmailNotification(Long notificationId, String to, String title, String message, String url) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림이 존재하지 않습니다."));
        NotificationMethodCode emailMethod = methodCodeRepository.findByCode("EMAIL");

        boolean isSent = false;
        String status = "FAIL";
        String detail = null;

        String subject = "[FairPlay] " + title;
        String html = buildNotificationEmailHtml(title, message, url);

        try {
            emailSender.send(to, subject, html, "logo", "/static/logo.png");
            isSent = true;
            status = "SUCCESS";
        } catch (Exception e) {
            detail = e.getMessage();
            log.warn("알림 이메일 전송 실패: {}", detail);
        }

        NotificationLog log = NotificationLog.builder()
                .notification(notification)
                .methodCode(emailMethod)
                .isSent(isSent)
                .status(status)
                .detail(detail)
                .build();

        notificationLogRepository.save(log);
    }

    /**
     * 알림(일반) 이메일 템플릿 읽기 및 동적 치환
     * @param title   알림 제목
     * @param message 알림 본문
     * @param url     상세 보기 링크 (null/blank이면 버튼 미노출)
     * @return 치환된 HTML 본문
     */
    private String buildNotificationEmailHtml(String title, String message, String url) {
        try (InputStream is = new ClassPathResource("email/notification.html").getInputStream()) {
            String template = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            String buttonHtml = (url != null && !url.isBlank())
                    ? String.format("<div style=\"text-align:center; margin-top:16px;\"><a href=\"%s\" class=\"link-btn\">상세보기</a></div>", url)
                    : "";
            return String.format(template, title, message, buttonHtml);
        } catch (Exception e) {
            throw new RuntimeException("알림 이메일 템플릿 로딩 실패", e);
        }
    }

    // AbstractEmailService 템플릿 미사용 (별도 구현)
    @Override
    protected EmailContent createEmailContent(Object... params) {
        return null;
    }
}
