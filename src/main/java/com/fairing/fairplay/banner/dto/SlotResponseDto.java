package com.fairing.fairplay.banner.dto;
import com.fairing.fairplay.banner.entity.BannerSlotStatus;

import java.time.LocalDate;

public record SlotResponseDto(
        LocalDate slotDate,
        Integer priority,
        BannerSlotStatus status,     // AVAILABLE / LOCKED / SOLD
        Integer price
) {}