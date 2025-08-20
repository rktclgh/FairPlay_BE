package com.fairing.fairplay.banner.controller;


import com.fairing.fairplay.banner.batch.HotPickScheduler;
import com.fairing.fairplay.banner.batch.NewPickScheduler;
import com.fairing.fairplay.banner.service.BannerService;
import com.fairing.fairplay.core.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/batch")
@RequiredArgsConstructor
public class AdminBannerBatchController {
    private static final String ROLE_ADMIN = "ADMIN";
    private final BannerService bannerService;

    private final NewPickScheduler newPickScheduler;
    private final HotPickScheduler hotPickScheduler;

    private void requireAdmin(CustomUserDetails user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        if (!ROLE_ADMIN.equals(user.getRoleCode())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자만 접근할 수 있습니다.");
        }
    }

    @PostMapping("/new-picks/run")
    public ResponseEntity<Void> runNewPicks(@AuthenticationPrincipal CustomUserDetails user) {
            requireAdmin(user);
        newPickScheduler.updateNewPicks();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/hot-picks/run")
    public ResponseEntity<Void> runHotPicks(@AuthenticationPrincipal CustomUserDetails user) {
            requireAdmin(user);
        hotPickScheduler.updateHotPicks();
        return ResponseEntity.ok().build();
    }
}