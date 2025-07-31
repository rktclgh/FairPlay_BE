package com.fairing.fairplay.user.service;

import com.fairing.fairplay.user.dto.EmailVerificationRequestDto;
import com.fairing.fairplay.user.dto.EmailCodeVerifyRequestDto;
import com.fairing.fairplay.user.entity.EmailVerification;
import com.fairing.fairplay.user.repository.EmailVerificationRepository;
import com.fairing.fairplay.core.util.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    <div style="font-family: 'Segoe UI', 'Apple SD Gothic Neo', Arial, sans-serif; max-width:480px; margin:0 auto; border:1.5px solid #dbeafe; border-radius:14px; box-shadow:0 4px 24px #e0e7ef60; padding:32px 20px 20px 20px; background:#f7fafc;">
        <div style="text-align:center; font-size:40px; margin-bottom:4px;">
            <span style="font-size:36px;">📬</span>
        </div>
        <h2 style="color:#2d3748; text-align:center; font-size:25px; margin-bottom:18px;">
            <span style="font-size:22px; color:#2563eb;">[FairPlay]</span> 이메일 인증 안내
        </h2>
        <div style="margin:18px 0 28px 0; padding:14px 0; border-radius:12px; background:#e0e7ef;">
            <p style="font-size:16px; color:#475569; text-align:center; margin:0;">
                👋 <b style="color:#2563eb;">FairPlay</b>를 이용해주셔서 감사합니다!<br>
                아래 <b style="color:#e11d48; font-size:20px;">인증코드</b>를<br>
                <b>5분 이내</b>에 입력해 주세요.
            </p>
        </div>
        <div style="font-size:38px; font-weight:800; color:#2563eb; letter-spacing:12px; background:#fff; border-radius:8px; box-shadow:0 0 8px #e0e7ef80; text-align:center; margin:16px 0 20px 0; padding:18px 0 10px 0;">
            %s
        </div>
        <ul style="font-size:14px; color:#4b5563; margin:22px 0 10px 10px; line-height:1.6; padding:0;">
            <li>⏳ 인증 유효시간: <b style="color:#e11d48;">5분</b></li>
            <li>🤫 <b>타인에게 인증코드를 절대 공유하지 마세요.</b></li>
            <li>💬 문의: <a href="mailto:support@fairplay.com" style="color:#2563eb; text-decoration:none;">support@fairplay.com</a></li>
        </ul>
        <div style="margin-top:24px; text-align:center;">
            <span style="font-size:11px; color:#a0aec0;">© FairPlay &middot; 박람회/행사 예약 플랫폼</span>
        </div>
    </div>
""".formatted(code);


        emailSender.send(dto.getEmail(), "[FairPlay] 이메일 인증코드", content);
    }

    // 인증코드 검증
    public boolean verifyCode(EmailCodeVerifyRequestDto dto) {
        EmailVerification entity = verificationRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일로 요청된 인증 없음"));

        if (!entity.getCode().equals(dto.getCode())) return false;
        if (entity.getExpiresAt().isBefore(LocalDateTime.now())) return false;

        entity.setVerified(true);
        verificationRepository.save(entity);
        log.info("\uD83D\uDCEC[이메일 인증] {} 인증 성공", dto.getEmail());
        return true;
    }

    private String generateCode() {
        Random random = new Random();
        int num = 100000 + random.nextInt(900000); // 6자리
        return String.valueOf(num);
    }
}
