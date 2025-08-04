package com.fairing.fairplay.core.email.repository;

import com.fairing.fairplay.core.email.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, String> {
    Optional<EmailVerification> findByEmail(String email);


    //오래된 인증 메일 삭제 쿼리!
    @Modifying
    @Query("delete from EmailVerification e where e.expiresAt < :now or e.verified = true")
    int deleteExpiredOrVerified(LocalDateTime now);
}
