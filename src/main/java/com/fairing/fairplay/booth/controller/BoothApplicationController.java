package com.fairing.fairplay.booth.controller;

import com.fairing.fairplay.booth.dto.*;
import com.fairing.fairplay.booth.service.BoothApplicationService;
import com.fairing.fairplay.core.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/booth/applications")
@RequiredArgsConstructor
public class BoothApplicationController {

    private final BoothApplicationService boothApplicationService;

    // 공통 권한 체크 메서드
    private void checkBoothManager(CustomUserDetails user) {
        System.out.println(" 현재 사용자 권한: " + user.getRoleCode());
        if (!"BOOTH_MANAGER".equals(user.getRoleCode())) {
            throw new AccessDeniedException("부스 관리자만 접근할 수 있습니다.");
        }
    }

    // 1. 고객 부스 신청 (권한 필요 없음)
    @PostMapping
    public ResponseEntity<Long> apply(@RequestBody BoothApplicationRequestDto dto) {
        Long id = boothApplicationService.applyBooth(dto);
        return ResponseEntity.created(URI.create("/api/booth/applications/" + id)).body(id);
    }

    // 2. 관리자 - 신청 목록 조회
    @GetMapping
    public ResponseEntity<List<BoothApplicationListDto>> getList(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam Long eventId) {

        checkBoothManager(user);
        List<BoothApplicationListDto> list = boothApplicationService.getBoothApplications(eventId);
        return ResponseEntity.ok(list);
    }

    // 3. 관리자 - 신청 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<BoothApplicationResponseDto> getDetail(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {

        checkBoothManager(user);
        BoothApplicationResponseDto dto = boothApplicationService.getBoothApplication(id);
        return ResponseEntity.ok(dto);
    }


    // 4. 관리자 - 승인/반려 처리
    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @RequestBody BoothApplicationStatusUpdateDto dto) {

        checkBoothManager(user);
        boothApplicationService.updateStatus(id, dto);
        return ResponseEntity.ok().build();
    }
}

