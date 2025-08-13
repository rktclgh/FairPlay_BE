// BannerSlotController.java
package com.fairing.fairplay.banner.controller;

import com.fairing.fairplay.banner.dto.SlotResponseDto;
import com.fairing.fairplay.banner.service.BannerSlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.ResponseEntity;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/banner")
public class BannerSlotController {

    private final BannerSlotService slotService;

    @GetMapping("/slots")
    /*public List<SlotResponseDto> getSlots(
            @RequestParam String type,  // "HERO", "SEARCH_TOP"*/
    public ResponseEntity<List<SlotResponseDto>> getSlots(
            @RequestParam
            @NotBlank(message = "타입은 필수입니다")
            @Pattern(regexp = "^(HERO|SEARCH_TOP)$", message = "타입은 HERO 또는 SEARCH_TOP이어야 합니다")
            String type,  // "HERO", "SEARCH_TOP"
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        if (from.isAfter(to)) {
                       throw new IllegalArgumentException("시작 날짜는 종료 날짜보다 이전이어야 합니다");
                    }
             return ResponseEntity.ok(slotService.getSlots(type, from, to));    }
}
