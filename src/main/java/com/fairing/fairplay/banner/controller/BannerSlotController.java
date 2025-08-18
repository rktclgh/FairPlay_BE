package com.fairing.fairplay.banner.controller;

import com.fairing.fairplay.banner.dto.SlotResponseDto;
import com.fairing.fairplay.banner.entity.BannerSlotType;
import com.fairing.fairplay.banner.service.BannerService;
import com.fairing.fairplay.banner.service.BannerSlotService;
import com.fairing.fairplay.core.security.CustomUserDetails;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.fairing.fairplay.banner.dto.LockSlotsRequestDto;
import com.fairing.fairplay.banner.dto.LockSlotsResponseDto;
import com.fairing.fairplay.banner.dto.FinalizeSoldRequestDto;
import com.fairing.fairplay.banner.dto.FinalizeSoldResponseDto;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/banner")
@Validated
public class BannerSlotController {
    private final BannerSlotService slotService;
    private final BannerService bannerService;

    private void requireLogin(CustomUserDetails user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
    }


    @GetMapping("/slots")
    public ResponseEntity<List<SlotResponseDto>> getSlots(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam @NotNull(message = "배너 타입은 필수입니다") BannerSlotType type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {

        requireLogin(user);

        if (from.isAfter(to)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "시작 날짜는 종료 날짜보다 이전이어야 합니다"
            );
        }
        return ResponseEntity.ok(slotService.getSlots(type, from, to));



    }
    @PostMapping("/slots/lock")
    public ResponseEntity<LockSlotsResponseDto> lockSlots(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody LockSlotsRequestDto req
    ) {
        requireLogin(user);
        return ResponseEntity.ok(bannerService.lockSlots(req, user.getUserId()));
    }

    @PostMapping("/slots/sold")
    public ResponseEntity<FinalizeSoldResponseDto> finalizeSold(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody FinalizeSoldRequestDto req
    ) {
        requireLogin(user);
        return ResponseEntity.ok(bannerService.finalizeSold(req, user.getUserId()));
    }

}