package com.fairing.fairplay.banner.controller;

import com.fairing.fairplay.banner.dto.AdminApplicationListItemDto;
import com.fairing.fairplay.banner.service.BannerApplicationService;
import com.fairing.fairplay.banner.service.NewBannerApplicationService;
import com.fairing.fairplay.core.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final NewBannerApplicationService newBannerService;

    private void requireLogin(CustomUserDetails user) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        // TODO: 필요하면 ROLE_ADMIN 체크
    }

    //  관리자 목록: type/status 없거나 all이면 null로 전달 → 두 타입 모두 조회
    @GetMapping("/applications")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<AdminApplicationListItemDto>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            Pageable pageable
    ) {
        String s = normalizeStatus(status); // null | PENDING | APPROVED | REJECTED
        String t = normalizeType(type);     // null | HERO | SEARCH_TOP
        return ResponseEntity.ok(appService.listAdminApplications(s, t, pageable));
    }

    //  광고 신청 승인(부스 방식처럼 승인 후 결제 링크 이메일 발송)
    @PostMapping("/applications/{id}/approve")
    public ResponseEntity<AdminApplicationListItemDto> approve(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        requireLogin(user);
        // 기존 markPaid 대신 새로운 승인 메서드 사용 (결제 링크 이메일 발송)
        appService.approveWithPaymentLink(id, user.getUserId());
        return ResponseEntity.ok(appService.getAdminApplicationView(id));
    }

    //  승인(기존 방식 - 승인과 동시에 결제 완료 처리)
    @PostMapping("/applications/{id}/approve-and-pay")
    public ResponseEntity<AdminApplicationListItemDto> approveAndPay(
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

    /**
     * 하드 딜리트 (완전 삭제 - 관리자 전용)
     * new_ 테이블 기반의 새로운 서비스 사용
     */
    @DeleteMapping("/applications/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, String>> hardDelete(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        requireLogin(user);
        String reason = body != null ? body.getOrDefault("reason", "관리자 삭제") : "관리자 삭제";
        
        try {
            // 새로운 배너 시스템의 하드 삭제 기능 사용
            newBannerService.hardDeleteApplication(id, user.getUserId(), reason);
            return ResponseEntity.ok(Map.of(
                "message", "배너 신청이 완전히 삭제되었습니다.",
                "deletedId", id.toString(),
                "deletedBy", user.getUserId().toString(),
                "reason", reason
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "삭제 실패: " + e.getMessage()
            ));
        }
    }

    /**
     * new_ 테이블 기반 배너 신청 승인 (새로운 시스템)
     */
    @PostMapping("/new-applications/{id}/approve")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, String>> approveNewApplication(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        requireLogin(user);
        String comment = body != null ? body.getOrDefault("comment", "") : "";
        
        try {
            newBannerService.approveApplication(id, user.getUserId(), comment);
            return ResponseEntity.ok(Map.of(
                "message", "배너 신청이 승인되었습니다.",
                "approvedId", id.toString(),
                "approvedBy", user.getUserId().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "승인 실패: " + e.getMessage()
            ));
        }
    }

    /**
     * new_ 테이블 기반 배너 신청 반려 (새로운 시스템)
     */
    @PostMapping("/new-applications/{id}/reject")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, String>> rejectNewApplication(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        requireLogin(user);
        String reason = body != null ? body.getOrDefault("reason", "승인 기준에 부합하지 않습니다.") : "승인 기준에 부합하지 않습니다.";
        
        try {
            newBannerService.rejectApplication(id, user.getUserId(), reason);
            return ResponseEntity.ok(Map.of(
                "message", "배너 신청이 반려되었습니다.",
                "rejectedId", id.toString(),
                "rejectedBy", user.getUserId().toString(),
                "reason", reason
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "반려 실패: " + e.getMessage()
            ));
        }
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
