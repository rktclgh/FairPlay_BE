package com.fairing.fairplay.core.email.controller;

import com.fairing.fairplay.core.email.dto.EmailVerificationRequestDto;
import com.fairing.fairplay.core.email.dto.EmailCodeVerifyRequestDto;
import com.fairing.fairplay.core.email.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailVerificationController {
    private final EmailVerificationService verificationService;

    // 인증코드 발송
    @PostMapping("/send-verification")
    public ResponseEntity<Void> sendVerification(@RequestBody @Valid EmailVerificationRequestDto dto) {
        verificationService.sendVerificationCode(dto);
        return ResponseEntity.ok().build();
    }

    // 인증코드 검증
    @PostMapping("/verify-code")
    public ResponseEntity<Void> verifyCode(@RequestBody @Valid EmailCodeVerifyRequestDto dto) {
        verificationService.verifyCode(dto);
        return ResponseEntity.ok().build();
    }
}
