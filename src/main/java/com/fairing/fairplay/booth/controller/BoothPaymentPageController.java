package com.fairing.fairplay.booth.controller;

import com.fairing.fairplay.booth.dto.BoothPaymentPageDto;
import com.fairing.fairplay.booth.entity.BoothApplication;
import com.fairing.fairplay.booth.repository.BoothApplicationRepository;
import com.fairing.fairplay.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/booths/payment-page")
@RequiredArgsConstructor
public class BoothPaymentPageController {

    private final BoothApplicationRepository boothApplicationRepository;

    // 부스 결제 페이지 정보 조회 (이메일 링크에서 접근)
    @GetMapping("/{applicationId}")
    public ResponseEntity<BoothPaymentPageDto> getBoothPaymentInfo(@PathVariable Long applicationId) {
        
        BoothApplication application = boothApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "부스 신청 정보를 찾을 수 없습니다."));
        
        // 승인된 신청만 결제 가능
        if (!"APPROVED".equals(application.getBoothApplicationStatusCode().getCode())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "승인되지 않은 부스 신청입니다.");
        }
        
        // 이미 결제 완료된 경우
        if ("PAID".equals(application.getBoothPaymentStatusCode().getCode())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "이미 결제가 완료된 부스입니다.");
        }
        
        BoothPaymentPageDto paymentInfo = BoothPaymentPageDto.builder()
                .applicationId(application.getId())
                .eventTitle(application.getEvent().getTitleKr())
                .boothTitle(application.getBoothTitle())
                .boothTypeName(application.getBoothType().getName())
                .boothTypeSize(application.getBoothType().getSize())
                .price(application.getBoothType().getPrice())
                .managerName(application.getManagerName())
                .contactEmail(application.getContactEmail())
                .paymentStatus(application.getBoothPaymentStatusCode().getCode())
                .build();
        
        return ResponseEntity.ok(paymentInfo);
    }
}