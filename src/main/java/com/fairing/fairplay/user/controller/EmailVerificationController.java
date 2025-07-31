package com.fairing.fairplay.user.controller;

import com.fairing.fairplay.user.dto.EmailVerificationRequestDto;
import com.fairing.fairplay.user.dto.EmailCodeVerifyRequestDto;
import com.fairing.fairplay.user.service.EmailVerificationService;
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
        boolean verified = verificationService.verifyCode(dto);
        return verified ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }
}
