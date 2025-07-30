package com.fairing.fairplay.banner.controller;

import com.fairing.fairplay.banner.dto.*;
import com.fairing.fairplay.banner.service.BannerService;
import com.fairing.fairplay.admin.entity.AdminAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import java.net.URI;

@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
public class AdminBannerController {

    private final BannerService bannerService;

    // TODO: 인증된 관리자 계정 주입 방식은 현재는 파라미터 직접 전달 (임시)
    // 추후 Spring Security 연동 시 @AuthenticationPrincipal 사용 가능

    // 배너 등록
    @PostMapping
    public ResponseEntity<BannerResponseDto> createBanner(@RequestBody BannerRequestDto dto) {
        AdminAccount admin = getDummyAdmin(); // 테스트용 관리자
        BannerResponseDto response = bannerService.createBanner(dto, admin);
        return ResponseEntity.created(URI.create("/api/admin/banners/" + response.getId())).body(response);
    }

    // 배너 수정
    @PutMapping("/{id}")
    public ResponseEntity<BannerResponseDto> updateBanner(
            @PathVariable Long id,
            @RequestBody BannerRequestDto dto
    ) {
        AdminAccount admin = getDummyAdmin();
        BannerResponseDto response = bannerService.updateBanner(id, dto, admin);
        return ResponseEntity.ok(response);
    }

    //배너 상태 ON/OFF 전환
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long id,
            @RequestBody BannerStatusUpdateDto dto
    ) {
        AdminAccount admin = getDummyAdmin();
        bannerService.changeStatus(id, dto, admin);
        return ResponseEntity.ok().build();
    }

    //배너 우선순위 변경
    @PatchMapping("/{id}/priority")
    public ResponseEntity<Void> updatePriority(
            @PathVariable Long id,
            @RequestBody BannerPriorityUpdateDto dto
    ) {
        AdminAccount admin = getDummyAdmin();
        bannerService.changePriority(id, dto, admin);
        return ResponseEntity.ok().build();
    }

    //전체 배너 목록 (옵션)
    @GetMapping
    public ResponseEntity<List<BannerResponseDto>> listAll() {
        List<BannerResponseDto> banners = bannerService.getAllBanners();
        return ResponseEntity.ok(banners);
    }

    // 테스트용 더미 관리자 계정
    private AdminAccount getDummyAdmin() {
        return new AdminAccount(1L);
    }

}
