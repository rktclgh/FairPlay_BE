package com.fairing.fairplay.core.email.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.email.dto.EmailVerificationRequestDto;
import com.fairing.fairplay.core.email.dto.EmailCodeVerifyRequestDto;
import com.fairing.fairplay.core.email.entity.EmailVerification;
import com.fairing.fairplay.core.email.repository.EmailVerificationRepository;
import com.fairing.fairplay.core.email.entity.EmailServiceFactory;
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
    private final EmailServiceFactory emailServiceFactory; // 변경

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

        log.info("[이메일 인증] 인증코드 발송 - email={}, code= {}", dto.getEmail(), code);

        // 책임 분리: email 모듈에서 인증 메일 발송
        emailServiceFactory.getService(EmailServiceFactory.EmailType.VERIFICATION)
                .send(dto.getEmail(), code);
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
        log.info("[이메일 인증] {} 인증 성공", dto.getEmail());
    }

    private String generateCode() {
        Random random = new Random();
        int num = 100000 + random.nextInt(900000); // 6자리
        return String.valueOf(num);
    }
}
