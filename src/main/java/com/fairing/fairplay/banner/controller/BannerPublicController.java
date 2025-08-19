package com.fairing.fairplay.banner.controller;

import com.fairing.fairplay.banner.dto.HotPickDto;
import com.fairing.fairplay.banner.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/banner")
@RequiredArgsConstructor
public class BannerPublicController {
    private final BannerService bannerService;

    @GetMapping("/hot-picks")
    public ResponseEntity<List<HotPickDto>> getHotPicks(
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        int limit = Math.max(1, Math.min(size, 20)); // 1~20 방어
        return ResponseEntity.ok(bannerService.getActiveHotPicks(limit));
    }
}
