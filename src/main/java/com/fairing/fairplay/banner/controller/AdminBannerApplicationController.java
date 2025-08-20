package com.fairing.fairplay.banner.controller;

import com.fairing.fairplay.banner.dto.AdminApplicationListItemDto;
import com.fairing.fairplay.banner.service.BannerApplicationService;
import com.fairing.fairplay.core.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/banners")
public class AdminBannerApplicationController {

    private final BannerApplicationService appService;

    private void requireLogin(CustomUserDetails user) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        // TODO: 필요하면 ROLE_ADMIN 체크
    }

    //  관리자 목록: type/status 없거나 all이면 null로 전달 → 두 타입 모두 조회
    @GetMapping("/applications")
    public ResponseEntity<List<AdminApplicationListItemDto>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        String s = normalizeStatus(status); // null | PENDING | APPROVED | REJECTED
        String t = normalizeType(type);     // null | HERO | SEARCH_TOP
        return ResponseEntity.ok(appService.listAdminApplications(s, t, page, size));
    }

    //  승인(= 결제 성공 처리 + 배너 생성)
    @PostMapping("/applications/{id}/approve")
    public ResponseEntity<AdminApplicationListItemDto> approve(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        requireLogin(user);
        appService.markPaid(id, user.getUserId());
        return ResponseEntity.ok(appService.getAdminApplicationView(id));
    }

    //  반려(잠금 해제 + 상태 변경)
    @PostMapping("/applications/{id}/reject")
    public ResponseEntity<AdminApplicationListItemDto> reject(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        requireLogin(user);
        String reason = body != null ? body.getOrDefault("reason", "") : "";
        appService.reject(id, user.getUserId(), reason);
        return ResponseEntity.ok(appService.getAdminApplicationView(id));
    }

    // ---- helpers ----
    private String normalizeStatus(String s) {
        if (s == null || s.isBlank() || s.equalsIgnoreCase("all")) return null;
        s = s.toUpperCase();
        return switch (s) {
            case "PENDING", "APPROVED", "REJECTED" -> s;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid status");
        };
    }

    private String normalizeType(String t) {
        if (t == null || t.isBlank() || t.equalsIgnoreCase("all")) return null;
        t = t.toUpperCase();
        return switch (t) {
            case "HERO", "MAIN", "MAIN_BANNER", "MAINBANNER" -> "HERO";
            case "SEARCH_TOP", "SEARCHTOP", "MDPICK", "MD_PICK" -> "SEARCH_TOP";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid type");
        };
    }
}
