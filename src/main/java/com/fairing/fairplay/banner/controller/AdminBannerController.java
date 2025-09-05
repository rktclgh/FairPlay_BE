package com.fairing.fairplay.banner.controller;

import com.fairing.fairplay.banner.dto.*;
import com.fairing.fairplay.banner.service.BannerApplicationService;
import com.fairing.fairplay.banner.service.BannerService;
import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.history.etc.ChangeBanner;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public ResponseEntity<Map<String, Object>> summary(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "7") int expiringDays,                 // 옵션: 기본 7일
            @RequestParam(required = false) String expiringType                 // 옵션: 타입별 계산 원하면 전달 (HERO/MD_PICK/...)
    ) {
        requireAdmin(user);
        Map<String, Object> result = new HashMap<>();
        result.put("totalSales", bannerService.sumBannerSales());       // 기존
        result.put("activeCount", bannerService.countActiveBannersNow());
        result.put("recentCount", bannerService.countRecentBanners(7));

        long expiring = (expiringType == null || expiringType.isBlank())
                ? bannerService.countExpiringAll(expiringDays)          // 전체 타입
                : bannerService.countExpiringByType(expiringDays, expiringType);

        result.put("expiringCount", expiring);                          // 추가
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
    
    // 배너 하드 삭제 (슬롯을 AVAILABLE로 되돌림)
    @DeleteMapping("/{id}")
    @FunctionAuth("deleteBanner")
    @ChangeBanner("배너 삭제")
    public ResponseEntity<Map<String, String>> deleteBanner(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {
        requireAdmin(user);
        String result = bannerService.hardDeleteBanner(id, user.getUserId());
        Map<String, String> response = new HashMap<>();
        response.put("message", result);
        return ResponseEntity.ok(response);
    }

    // 관리자용 신청서 목록 조회
    @GetMapping(value = "/applications", params = "!id")
    public ResponseEntity<?> listApplications(
            @AuthenticationPrincipal CustomUserDetails admin,
            @RequestParam(required = false) String status,     // PENDING | APPROVED | REJECTED
            @RequestParam(required = false) String type,       // HERO | SEARCH_TOP
            Pageable pageable
    ) {
                requireAdmin(admin);
                return ResponseEntity.ok(appService.listAdminApplications(status, type, pageable));
            }

}
