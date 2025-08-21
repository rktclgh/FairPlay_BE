package com.fairing.fairplay.banner.controller;

import com.fairing.fairplay.banner.dto.BannerResponseDto;
import com.fairing.fairplay.banner.dto.HotPickDto;
import com.fairing.fairplay.banner.dto.NewPickDto;
import com.fairing.fairplay.banner.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/banners") // ← 한 가지로 통일(권장)
@RequiredArgsConstructor
public class BannerPublicController {
    private final BannerService bannerService;

    @GetMapping("/hero/active")
    @PreAuthorize("permitAll()")
    public List<BannerResponseDto> getHeroActive() {
        return bannerService.getHeroActive();
    }

    @GetMapping("/new-picks")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<NewPickDto>> getNewPicks(
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        int limit = Math.max(1, Math.min(size, 20));
        return ResponseEntity.ok(bannerService.getActiveNewPicks(limit));
    }

    @GetMapping
    @PreAuthorize("permitAll()")
    public List<BannerResponseDto> findByTypeAndStatus(
            @RequestParam String type,
            @RequestParam String status) {
        return bannerService.findByTypeAndStatus(type, status);
    }

    @GetMapping("/hot-picks")
    @PreAuthorize("permitAll()")   // ← 이 줄 추가
    public ResponseEntity<List<HotPickDto>> getHotPicks(
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        int limit = Math.max(1, Math.min(size, 20)); // 1~20 방어
        return ResponseEntity.ok(bannerService.getActiveHotPicks(limit));
    }
}
