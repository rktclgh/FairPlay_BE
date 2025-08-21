package com.fairing.fairplay.businesscard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessCardQRService {

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    /**
     * 전자명함 QR 코드 URL 생성
     */
    public String generateBusinessCardQR(Long userId) {
        // QR 코드에 포함될 URL 생성 (전자명함 수집 페이지)
        String cardUrl = frontendBaseUrl + "/business-card/collect/" + encodeUserId(userId);
        log.info("전자명함 QR URL 생성: {}", cardUrl);
        return cardUrl;
    }

    /**
     * 사용자 ID를 간단하게 인코딩 (보안을 위해)
     */
    private String encodeUserId(Long userId) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(userId.toString().getBytes());
    }

    /**
     * 인코딩된 사용자 ID를 디코딩
     */
    public Long decodeUserId(String encodedUserId) {
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(encodedUserId);
            return Long.valueOf(new String(decodedBytes));
        } catch (Exception e) {
            log.error("사용자 ID 디코딩 실패: {}", encodedUserId, e);
            throw new IllegalArgumentException("잘못된 QR 코드입니다.");
        }
    }
}