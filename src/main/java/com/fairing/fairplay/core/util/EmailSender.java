package com.fairing.fairplay.core.util;

import jakarta.activation.DataHandler;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.mail.util.ByteArrayDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Properties;

@Component
public class EmailSender {

    @Value("${spring.mail.host}")
    private String host;
    @Value("${spring.mail.port}")
    private String port;
    @Value("${spring.mail.username}")
    private String username;
    @Value("${spring.mail.password}")
    private String password;

    public void send(String to, String subject, String htmlContent) {
        send(to, subject, htmlContent, null, null);
    }

    public void send(String to, String subject, String htmlContent, String cid, String imagePath) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", true);
        props.put("mail.smtp.starttls.enable", true);
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);

            // 멀티파트 구조
            MimeMultipart multipart = new MimeMultipart("related");

            // HTML 본문
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlContent, "text/html; charset=utf-8");
            multipart.addBodyPart(htmlPart);

            // 인라인 이미지
            if (cid != null && imagePath != null) {
                MimeBodyPart imagePart = new MimeBodyPart();
                // ClassPathResource로 리소스 불러오기
                ClassPathResource resource = new ClassPathResource(imagePath);
                try (InputStream is = resource.getInputStream()) {
                    imagePart.setDataHandler(new DataHandler(new ByteArrayDataSource(is, "image/png")));
                }
                imagePart.setHeader("Content-ID", "<" + cid + ">");
                imagePart.setDisposition(MimeBodyPart.INLINE);
                multipart.addBodyPart(imagePart);
            }

            message.setContent(multipart);
            Transport.send(message);
        } catch (Exception e) {
            throw new RuntimeException("메일 전송 실패", e);
        }
    }
}
