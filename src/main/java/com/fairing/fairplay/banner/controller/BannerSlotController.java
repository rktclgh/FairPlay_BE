// BannerSlotController.java
package com.fairing.fairplay.banner.controller;

import com.fairing.fairplay.banner.dto.SlotResponseDto;
import com.fairing.fairplay.banner.service.BannerSlotService;
import com.fairing.fairplay.core.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/banner")
@Validated
public class BannerSlotController {
    private final BannerSlotService slotService;

    private void requireLogin(CustomUserDetails user) {
        if (user == null) throw new AccessDeniedException("로그인이 필요합니다.");
    }


    @GetMapping("/slots")
    public ResponseEntity<List<SlotResponseDto>> getSlots(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam
            @NotBlank(message = "타입은 필수입니다")
            @Pattern(regexp = "^(HERO|SEARCH_TOP)$", message = "타입은 HERO 또는 SEARCH_TOP이어야 합니다")
            String type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {

        requireLogin(user);

        if (from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "시작 날짜는 종료 날짜보다 이전이어야 합니다");
                    }
             return ResponseEntity.ok(slotService.getSlots(type, from, to));    }
}
