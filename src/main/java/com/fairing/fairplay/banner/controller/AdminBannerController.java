package com.fairing.fairplay.banner.controller;

import com.fairing.fairplay.banner.dto.*;
import com.fairing.fairplay.banner.service.BannerApplicationService;
import com.fairing.fairplay.banner.service.BannerService;
import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.core.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
public class AdminBannerController {

    private final BannerService bannerService;
    private final BannerApplicationService appService;

    // 공통 관리자 권한 체크
    private void requireAdmin(CustomUserDetails user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        if (!"ADMIN".equals(user.getRoleCode())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자만 접근할 수 있습니다.");
        }
    }


    // 배너 등록
    @FunctionAuth("createBanner")
    @PostMapping
    public ResponseEntity<BannerResponseDto> createBanner(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid BannerRequestDto requestDto) {

        requireAdmin(user);
        BannerResponseDto response = bannerService.createBanner(requestDto, user.getUserId());
        return ResponseEntity.ok(response);
    }

    // 배너 수정
    @FunctionAuth("updateBanner")
    @PutMapping("/{id}")
    public ResponseEntity<BannerResponseDto> updateBanner(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @RequestBody @Valid BannerRequestDto dto) {

        requireAdmin(user);
        BannerResponseDto response = bannerService.updateBanner(id, dto, user.getUserId());
        return ResponseEntity.ok(response);
    }

    // 배너 상태 ON/OFF 전환
    @FunctionAuth("updateBannerStatus")
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @RequestBody @Valid BannerStatusUpdateDto dto) {

        requireAdmin(user);
        bannerService.changeStatus(id, dto, user.getUserId());
        return ResponseEntity.ok().build();
    }

    // 배너 우선순위 변경
    @PatchMapping("/{id}/priority")
    @FunctionAuth("updatePriority")
    public ResponseEntity<Void> updatePriority(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @RequestBody @Valid BannerPriorityUpdateDto dto) {

        requireAdmin(user);
        bannerService.changePriority(id, dto, user.getUserId());
        return ResponseEntity.ok().build();
    }

    // 전체 배너 목록
    @GetMapping
    @FunctionAuth("listAll")
    public ResponseEntity<List<BannerResponseDto>> listAll(
            @AuthenticationPrincipal CustomUserDetails user) {

        requireAdmin(user);
        List<BannerResponseDto> banners = bannerService.getAllBanners();
        return ResponseEntity.ok(banners);
    }

    // 결제 성공 처리(승인 → SOLD + 배너 생성)
    @PostMapping("/applications/{id}/mark-paid")
    public ResponseEntity<Void> markPaid(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {
        requireAdmin(user);
        appService.markPaid(id, user.getUserId()); // X-Admin-Id 대신 로그인 사용자 사용
        return ResponseEntity.ok().build();
    }
}
