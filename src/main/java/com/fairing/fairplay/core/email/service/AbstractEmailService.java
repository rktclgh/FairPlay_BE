package com.fairing.fairplay.core.email.service;

import com.fairing.fairplay.core.util.EmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public abstract class AbstractEmailService {
    protected final EmailSender emailSender;

    // 공통 메일 전송
    public void send(String to, Object... params) {
        EmailContent content = createEmailContent(params);
        emailSender.send(to, content.getSubject(), content.getHtml(), content.getLogoCid(), content.getLogoPath());
    }

    // 각 서비스별 구현
    protected abstract EmailContent createEmailContent(Object... params);

    // 템플릿 로더
    protected String loadTemplate(String filename) {
        // 파일명 검증: 상위 디렉토리 접근 방지
        if (filename == null || filename.contains("..") || filename.contains("/") || filename.contains("\\") ) {
            throw new IllegalArgumentException("잘못된 템플릿 파일명: " + filename);
        }
        try (InputStream is = new ClassPathResource("email/" + filename).getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("이메일 템플릿 파일 로딩 실패: " + filename, e);
        }
    }


    // 컨텐츠 구조체
    public static class EmailContent {
        private final String subject;
        private final String html;
        private final String logoCid;
        private final String logoPath;

        public EmailContent(String subject, String html, String logoCid, String logoPath) {
            this.subject = subject;
            this.html = html;
            this.logoCid = logoCid;
            this.logoPath = logoPath;
        }

        public String getSubject() { return subject; }
        public String getHtml() { return html; }
        public String getLogoCid() { return logoCid; }
        public String getLogoPath() { return logoPath; }
    }
}
