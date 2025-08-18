package com.fairing.fairplay.booth.controller;

import com.fairing.fairplay.booth.dto.*;
import com.fairing.fairplay.booth.service.BoothApplicationService;
import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.core.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}/booths/apply")
@RequiredArgsConstructor
public class BoothApplicationController {

    private final BoothApplicationService boothApplicationService;

    // 공통 권한 체크 메서드
    private void checkEventManager(CustomUserDetails user) {
        System.out.println(" 현재 사용자 권한: " + user.getRoleCode());
        if (!"EVENT_MANAGER".equals(user.getRoleCode())) {
            throw new AccessDeniedException("행사 관리자만 접근할 수 있습니다.");
        }
    }

    private void checkBoothManager(CustomUserDetails user) {
        System.out.println("현재 사용자 권한: " + user.getRoleCode());
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
    @FunctionAuth("getList")
    @GetMapping
    public ResponseEntity<List<BoothApplicationListDto>> getList(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam Long eventId) {

        checkEventManager(user);
        List<BoothApplicationListDto> list = boothApplicationService.getBoothApplications(eventId);
        return ResponseEntity.ok(list);
    }

    // 3. 관리자 - 신청 상세 조회
    @GetMapping("/{id}")
    @FunctionAuth("getDetail")
    public ResponseEntity<BoothApplicationResponseDto> getDetail(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {

        checkEventManager(user);
        BoothApplicationResponseDto dto = boothApplicationService.getBoothApplication(id);
        return ResponseEntity.ok(dto);
    }

    // 4. 관리자 - 승인/반려 처리
    @PutMapping("/{id}/status")
    @FunctionAuth("updateStatus")
    public ResponseEntity<Void> updateStatus(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @RequestBody BoothApplicationStatusUpdateDto dto) {

        checkEventManager(user);
        boothApplicationService.updateStatus(id, dto);
        return ResponseEntity.ok().build();
    }

    // 5. 행사 관리자 - 결제 상태 변경 처리
    @PutMapping("/{id}/payment-status")
    @FunctionAuth("updatePaymentStatus")
    public ResponseEntity<Void> updatePaymentStatus(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @RequestBody BoothPaymentStatusUpdateDto dto) {

        checkEventManager(user);
        boothApplicationService.updatePaymentStatus(id, dto);
        return ResponseEntity.ok().build();
    }

    // 6. 부스 관리자 - 취소 요청 (태스트 x)
    @PutMapping("/{id}/cancel")
    @FunctionAuth("cancelApplication")
    public ResponseEntity<Void> cancelApplication(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {

        checkBoothManager(user);
        boothApplicationService.cancelApplication(id, user.getUserId());
        return ResponseEntity.ok().build();
    }

}
