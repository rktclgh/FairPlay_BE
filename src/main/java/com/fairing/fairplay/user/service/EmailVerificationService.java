package com.fairing.fairplay.user.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.user.dto.EmailVerificationRequestDto;
import com.fairing.fairplay.user.dto.EmailCodeVerifyRequestDto;
import com.fairing.fairplay.user.entity.EmailVerification;
import com.fairing.fairplay.user.repository.EmailVerificationRepository;
import com.fairing.fairplay.core.util.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {
    private final EmailVerificationRepository verificationRepository;
    private final EmailSender emailSender;

    // 인증코드 생성 및 메일 전송
    public void sendVerificationCode(EmailVerificationRequestDto dto) {
        String code = generateCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

        EmailVerification entity = EmailVerification.builder()
                .email(dto.getEmail())
                .code(code)
                .expiresAt(expiresAt)
                .verified(false)
                .build();
        verificationRepository.save(entity);

        log.info("\uD83D\uDCEC[이메일 인증] 인증코드 발송 - email={}, code= {}", dto.getEmail(), code);
        // HTML 인증 메일
        String content = """
<!doctype html>
<html lang="ko">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>FairPlay 이메일 인증 템플릿 테스트</title>
    <style>
      body {
        font-family: "Segoe UI", "Apple SD Gothic Neo", Arial, sans-serif;
        background: #f1f5f9;
        margin: 0;
        padding: 20px;
      }
      .container {
        max-width: 400px;
        margin: 0 auto;
        border: 1px solid #e2e8f0;
        border-radius: 16px;
        box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
        padding: 40px 24px 32px 24px;
        background: #ffffff;
      }
      .logo {
        text-align: center;
        margin-bottom: 24px;
      }
      .logo-icon {
        width: 64px;
        height: 64px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 16px;
      }
      .logo-icon span {
        font-size: 28px;
      }
      h2 {
        color: #1f2937;
        text-align: center;
        font-size: 24px;
        font-weight: 600;
        margin: 0;
      }
      .brand {
        color: #3b82f6;
        font-weight: 700;
      }
      .message-box {
        margin: 24px 0;
        padding: 20px;
        border-radius: 12px;
        background: linear-gradient(135deg, #f8fafc, #f1f5f9);
        border: 1px solid #e2e8f0;
      }
      .message-box p {
        font-size: 15px;
        color: #475569;
        text-align: center;
        margin: 0;
        line-height: 1.6;
      }
      .highlight {
        color: #3b82f6;
      }
      .code-highlight {
        color: #dc2626;
        font-size: 18px;
      }
      .verification-code {
        font-size: 32px;
        font-weight: 700;
        color: #3b82f6;
        letter-spacing: 8px;
        background: #f8fafc;
        border: 2px solid #e2e8f0;
        border-radius: 12px;
        text-align: center;
        margin: 20px 0 24px 0;
        padding: 24px 16px;
        font-family: "Courier New", monospace;
      }
      .warning-box {
        background: #fef2f2;
        border: 1px solid #fecaca;
        border-radius: 8px;
        padding: 16px;
        margin: 20px 0;
      }
      .warning-box ul {
        font-size: 13px;
        color: #4b5563;
        margin: 0;
        line-height: 1.5;
        padding-left: 20px;
      }
      .warning-box li {
        margin-bottom: 6px;
      }
      .warning-highlight {
        color: #dc2626;
      }
      .contact-link {
        color: #3b82f6;
        text-decoration: none;
        font-weight: 500;
      }
      .footer {
        margin-top: 32px;
        text-align: center;
        padding-top: 20px;
        border-top: 1px solid #f1f5f9;
      }
      .footer span {
        font-size: 12px;
        color: #9ca3af;
      }
    </style>
  </head>
  <body>
    <div class="container">
      <div class="logo">
        <div class="logo-icon">
          <img
            src="cid:logo"
            alt="FairPlay Logo"
            style="width: 64px; height: 64px; object-fit: contain"
          />
        </div>
        <h2><span class="brand">FairPlay</span> 이메일 인증</h2>
      </div>

      <div class="message-box">
        <p>
          안녕하세요! <strong class="highlight">FairPlay</strong>를 이용해주셔서
          감사합니다.<br />
          아래 <strong class="code-highlight">인증코드</strong>를<br />
          <strong>5분 이내</strong>에 입력해 주세요.
        </p>
      </div>

      <div class="verification-code">%s</div>

      <div class="warning-box">
        <ul>
          <li>
            ⏰ <strong class="warning-highlight">인증 유효시간: 5분</strong>
          </li>
          <li>🔒 <strong>타인에게 인증코드를 절대 공유하지 마세요</strong></li>
          <li>
            📞 문의:
            <a href="support@fairplay.com" class="contact-link"
              >support@fair-play.ink</a
            >
          </li>
        </ul>
      </div>

      <div class="footer">
        <span>© 2025 FairPlay · 박람회/행사 예약 플랫폼</span>
      </div>
    </div>
  </body>
</html>
""".formatted(code);



        emailSender.send(dto.getEmail(), "[FairPlay] 이메일 인증코드", content, "logo", "etc/logo.png");
    }

    // 인증코드 검증
    @Transactional
    public void verifyCode(EmailCodeVerifyRequestDto dto) {
        EmailVerification entity = verificationRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "인증 요청을 찾을 수 없습니다."));

        if (entity.getVerified()) {
            return; // 이미 인증됨
        }

        if (entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new CustomException(HttpStatus.GONE, "인증 코드가 만료되었습니다. 다시 요청해주세요.");
        }

        if (!entity.getCode().equals(dto.getCode())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "인증번호가 일치하지 않습니다.");
        }

        entity.setVerified(true);
        verificationRepository.save(entity);
        log.info("\uD83D\uDCEC[이메일 인증] {} 인증 성공", dto.getEmail());
    }

    private String generateCode() {
        Random random = new Random();
        int num = 100000 + random.nextInt(900000); // 6자리
        return String.valueOf(num);
    }
}
