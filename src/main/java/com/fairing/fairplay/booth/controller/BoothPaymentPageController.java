package com.fairing.fairplay.booth.controller;

import com.fairing.fairplay.booth.dto.BoothPaymentPageDto;
import com.fairing.fairplay.booth.service.BoothApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/booths/payment-page")
@RequiredArgsConstructor
public class BoothPaymentPageController {

    private final BoothApplicationService boothApplicationService;

    // 부스 결제 페이지 정보 조회 (이메일 링크에서 접근)
    @GetMapping("/{applicationId}")
    public ResponseEntity<BoothPaymentPageDto> getBoothPaymentInfo(@PathVariable Long applicationId) {
        return ResponseEntity.ok(boothApplicationService.getBoothPaymentInfo(applicationId));
    }
}
