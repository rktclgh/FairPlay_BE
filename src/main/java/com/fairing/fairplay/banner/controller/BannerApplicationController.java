package com.fairing.fairplay.banner.controller;
import com.fairing.fairplay.banner.service.BannerApplicationService;
import org.springframework.http.HttpStatus;

import com.fairing.fairplay.banner.dto.CreateApplicationRequestDto;
import com.fairing.fairplay.core.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/banner")
public class BannerApplicationController {

    private final BannerApplicationService appService;

    private void requireLogin(CustomUserDetails user) {
              if (user == null) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
                }
    }

    @PostMapping("/applications")
    public ResponseEntity<Long> create(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid CreateApplicationRequestDto req) {

        requireLogin(user);
        Long id = appService.createApplicationAndLock(req, user.getUserId());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(id).toUri();

        // 생성 시 201 + Location
        return ResponseEntity.created(location).body(id);
    }

    @PostMapping("/applications/{id}/cancel")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {

        requireLogin(user);
        appService.cancelApplication(id, user.getUserId());
        // 본문 없음 → 204
        return ResponseEntity.noContent().build();
    }

}
