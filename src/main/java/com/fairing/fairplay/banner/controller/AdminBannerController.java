package com.fairing.fairplay.banner.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fairing.fairplay.banner.dto.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.fairing.fairplay.banner.service.BannerApplicationService;
import com.fairing.fairplay.banner.service.BannerService;
import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.history.etc.ChangeBanner;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
public class AdminBannerController {
    private static final String ROLE_ADMIN = "ADMIN";

    private final BannerService bannerService;
    private final BannerApplicationService appService;

    // 공통 관리자 권한 체크
    private void requireAdmin(CustomUserDetails user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        if (!ROLE_ADMIN.equals(user.getRoleCode())) {
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
    @ChangeBanner("배너 수정")
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
    @ChangeBanner("배너 상태 변경")
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
    @ChangeBanner("배너 우선순위 변경")
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
        return ResponseEntity.ok(bannerService.getAllBanners());
    }

    // 결제 성공 처리(승인 → SOLD + 배너 생성)
    @PostMapping("/applications/{id}/mark-paid")
    public ResponseEntity<Void> markPaid(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {
        requireAdmin(user);
        appService.markPaid(id, user.getUserId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> summary(@AuthenticationPrincipal CustomUserDetails user) {
        requireAdmin(user);
        Map<String, Object> result = new HashMap<>();
        result.put("totalSales", bannerService.sumBannerSales()); // HERO SOLD 합계
        result.put("activeCount", bannerService.countActiveBannersNow()); // HERO 현재 노출
        result.put("recentCount", bannerService.countRecentBanners(7));   // HERO 최근 7일 신규
        return ResponseEntity.ok(result);
    }

    @GetMapping("/vip")
    public ResponseEntity<List<BannerResponseDto>> listVip(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        requireAdmin(user);
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from은 to보다 빠르거나 같아야 합니다.");
        }
        return ResponseEntity.ok(bannerService.searchVip(type, status, q, from, to));
    }

    @PatchMapping("/reorder")
    public ResponseEntity<Void> reorderForDate(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid ReorderRequestDto req) {
        requireAdmin(user);
        bannerService.reorderForDate(req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<BannerResponseDto> getOne(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {
        requireAdmin(user);
        return ResponseEntity.ok(bannerService.getOne(id));
    }
}
