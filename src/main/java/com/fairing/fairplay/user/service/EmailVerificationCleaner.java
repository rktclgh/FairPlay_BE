package com.fairing.fairplay.user.service;

import com.fairing.fairplay.user.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class EmailVerificationCleaner {

    private final EmailVerificationRepository verificationRepository;

    // 매일 새벽 2시에 동작 (크론표현식: 초 분 시 일 월 요일)
    @Transactional
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanExpiredEmailVerifications() {
        int deleted = verificationRepository.deleteExpiredOrVerified(LocalDateTime.now());
        System.out.println("[스케줄러] 만료/인증완료 email_verification 삭제: " + deleted + "건");
    }
}
