package com.fairing.fairplay.booth.controller;

import com.fairing.fairplay.booth.dto.BoothApplicationListDto;
import com.fairing.fairplay.booth.service.BoothApplicationService;
import com.fairing.fairplay.core.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/booths")
@RequiredArgsConstructor
@Slf4j
public class BoothAdminController {

    private final BoothApplicationService boothApplicationService;

    // 부스 관리자 - 내 부스 신청 목록 조회
    @GetMapping("/my-applications")
    public ResponseEntity<List<BoothApplicationListDto>> getMyApplications(
            @AuthenticationPrincipal CustomUserDetails user) {
        
        List<BoothApplicationListDto> applications = boothApplicationService.getMyBoothApplications(user.getUserId());
        return ResponseEntity.ok(applications);
    }
}