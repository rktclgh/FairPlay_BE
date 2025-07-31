package com.fairing.fairplay.user.service;

import com.fairing.fairplay.user.dto.EmailVerificationRequestDto;
import com.fairing.fairplay.user.dto.EmailCodeVerifyRequestDto;
import com.fairing.fairplay.user.entity.EmailVerification;
import com.fairing.fairplay.user.repository.EmailVerificationRepository;
import com.fairing.fairplay.core.util.EmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
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

        emailSender.send(dto.getEmail(), "[FairPlay] 이메일 인증코드", "인증코드: " + code);
    }

    // 인증코드 검증
    public boolean verifyCode(EmailCodeVerifyRequestDto dto) {
        EmailVerification entity = verificationRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일로 요청된 인증 없음"));

        if (!entity.getCode().equals(dto.getCode())) return false;
        if (entity.getExpiresAt().isBefore(LocalDateTime.now())) return false;

        entity.setVerified(true);
        verificationRepository.save(entity);
        return true;
    }

    private String generateCode() {
        Random random = new Random();
        int num = 100000 + random.nextInt(900000); // 6자리
        return String.valueOf(num);
    }
}
