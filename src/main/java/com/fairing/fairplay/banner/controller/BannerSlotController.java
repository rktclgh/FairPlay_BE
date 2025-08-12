// BannerSlotController.java
package com.fairing.fairplay.banner.controller;

import com.fairing.fairplay.banner.dto.SlotResponseDto;
import com.fairing.fairplay.banner.service.BannerSlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/banner")
public class BannerSlotController {

    private final BannerSlotService slotService;

    @GetMapping("/slots")
    public List<SlotResponseDto> getSlots(
            @RequestParam String type,  // "HERO", "SEARCH_TOP"
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return slotService.getSlots(type, from, to);
    }
}
