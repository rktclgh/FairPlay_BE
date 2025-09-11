package com.fairing.fairplay.banner.controller;

import com.fairing.fairplay.banner.dto.BannerApplicationDto;
import com.fairing.fairplay.banner.service.NewBannerApplicationService;
import com.fairing.fairplay.core.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/host/banner-management")
@PreAuthorize("hasAnyAuthority('HOST', 'EVENT_MANAGER')")
@Slf4j
public class HostBannerManagementController {

    private final NewBannerApplicationService bannerApplicationService;

    private void requireLogin(CustomUserDetails user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
    }

    /**
     * 호스트의 배너 신청 목록 조회 (필터링 포함)
     * 날짜별, 상태별, 배너 타입별 필터링 지원
     */
    @GetMapping("/applications")
    public ResponseEntity<Page<BannerApplicationDto>> getMyBannerApplications(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String bannerType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        requireLogin(user);
        
        log.info("호스트 배너 신청 목록 조회 - 사용자: {}, 필터: status={}, bannerType={}, 날짜: {}~{}", 
                user.getUserId(), status, bannerType, startDate, endDate);

        Pageable pageable = PageRequest.of(page, size);
        Page<BannerApplicationDto> applications = bannerApplicationService.getHostApplications(
                user.getUserId(), status, bannerType, startDate, endDate, pageable);

        log.info("호스트 배너 신청 목록 조회 완료 - 총 {}건", applications.getTotalElements());
        return ResponseEntity.ok(applications);
    }

    /**
     * 특정 배너 신청서 상세 조회
     */
    @GetMapping("/applications/{applicationId}")
    public ResponseEntity<BannerApplicationDto> getBannerApplicationDetail(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long applicationId
    ) {
        requireLogin(user);
        
        log.info("배너 신청서 상세 조회 - 사용자: {}, 신청서 ID: {}", user.getUserId(), applicationId);

        BannerApplicationDto application = bannerApplicationService.getApplicationDetail(applicationId, user.getUserId());
        
        return ResponseEntity.ok(application);
    }

    /**
     * 결제 대기 상태인 배너 신청서의 결제 URL 생성
     * 이메일 확인 없이도 결제 가능하도록 함
     */
    @PostMapping("/applications/{applicationId}/payment-url")
    public ResponseEntity<Map<String, String>> generatePaymentUrl(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long applicationId
    ) {
        requireLogin(user);
        
        log.info("배너 결제 URL 생성 - 사용자: {}, 신청서 ID: {}", user.getUserId(), applicationId);

        try {
            // TODO: 본인 신청서인지 확인하는 로직 추가
            String paymentUrl = String.format("https://fair-play.ink/banner/payment?applicationId=%d", applicationId);
            
            return ResponseEntity.ok(Map.of(
                "paymentUrl", paymentUrl,
                "message", "결제 페이지 URL이 생성되었습니다."
            ));
            
        } catch (Exception e) {
            log.error("결제 URL 생성 실패 - 신청서 ID: {}", applicationId, e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "결제 URL 생성에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 배너 신청서 취소 (승인 대기 또는 결제 대기 상태만 가능)
     */
    @DeleteMapping("/applications/{applicationId}")
    public ResponseEntity<Map<String, String>> cancelBannerApplication(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long applicationId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        requireLogin(user);
        
        log.info("배너 신청서 취소 - 사용자: {}, 신청서 ID: {}", user.getUserId(), applicationId);

        String reason = body != null ? body.getOrDefault("reason", "호스트 취소") : "호스트 취소";

        try {
            bannerApplicationService.cancelApplication(applicationId, user.getUserId(), reason);
            
            return ResponseEntity.ok(Map.of(
                "message", "배너 신청이 취소되었습니다.",
                "cancelledId", applicationId.toString(),
                "reason", reason
            ));
            
        } catch (Exception e) {
            log.error("배너 신청서 취소 실패 - 신청서 ID: {}", applicationId, e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "신청 취소에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 호스트 배너 통계 정보 조회
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getBannerStatistics(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        requireLogin(user);
        
        log.info("호스트 배너 통계 조회 - 사용자: {}", user.getUserId());

        try {
            Map<String, Object> stats = bannerApplicationService.getHostStatistics(user.getUserId());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("배너 통계 조회 실패 - 사용자: {}", user.getUserId(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "통계 조회에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 배너 타입 목록 조회 (호스트 신청 시 선택용)
     */
    @GetMapping("/types")
    public ResponseEntity<Map<String, Object>> getBannerTypes(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        requireLogin(user);
        
        log.info("배너 타입 목록 조회 - 사용자: {}", user.getUserId());

        try {
            Map<String, Object> types = bannerApplicationService.getBannerTypes();
            
            return ResponseEntity.ok(types);
            
        } catch (Exception e) {
            log.error("배너 타입 조회 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "배너 타입 조회에 실패했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 배너 슬롯 가용성 조회
     */
    @GetMapping("/slots")
    public ResponseEntity<List<Map<String, Object>>> getBannerSlots(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam String type,
            @RequestParam String from,
            @RequestParam String to
    ) {
        requireLogin(user);
        
        log.info("배너 슬롯 조회 - 사용자: {}, 타입: {}, 기간: {} ~ {}", user.getUserId(), type, from, to);

        try {
            List<Map<String, Object>> slots = bannerApplicationService.getAvailableSlots(type, from, to);
            
            return ResponseEntity.ok(slots);
            
        } catch (Exception e) {
            log.error("배너 슬롯 조회 실패 - 타입: {}, 기간: {} ~ {}", type, from, to, e);
            return ResponseEntity.badRequest().body(List.of());
        }
    }

    /**
     * 새로운 배너 신청 생성
     */
    @PostMapping("/applications")
    public ResponseEntity<Long> createBannerApplication(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody Map<String, Object> requestBody
    ) {
        requireLogin(user);
        
        log.info("배너 신청 생성 - 사용자: {}, 요청: {}", user.getUserId(), requestBody);

        try {
            Long applicationId = bannerApplicationService.createApplication(user.getUserId(), requestBody);
            
            log.info("배너 신청 생성 완료 - 신청서 ID: {}", applicationId);
            return ResponseEntity.ok(applicationId);
            
        } catch (Exception e) {
            log.error("배너 신청 생성 실패 - 사용자: {}", user.getUserId(), e);
            return ResponseEntity.badRequest().body(null);
        }
    }
}