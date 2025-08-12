package com.fairing.fairplay.banner.controller;

import com.fairing.fairplay.banner.dto.CreateApplicationRequestDto;
import com.fairing.fairplay.banner.service.BannerApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/banner")
public class BannerApplicationController {

    private final BannerApplicationService appService;

    @PostMapping("/applications")
    public Long create(@RequestBody CreateApplicationRequestDto req,
                       @RequestHeader(value="X-User-Id") Long userId) {
        return appService.createApplicationAndLock(req, userId);
    }

    @PostMapping("/applications/{id}/cancel")
    public void cancel(@PathVariable Long id,
                       @RequestHeader(value="X-User-Id") Long userId) {
        appService.cancelApplication(id, userId);
    }
}
