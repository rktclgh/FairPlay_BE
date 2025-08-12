package com.fairing.fairplay.banner.dto;

import java.time.LocalDate;

public record SlotResponseDto(
        LocalDate slotDate,
        Integer priority,
        String status,     // AVAILABLE / LOCKED / SOLD
        Integer price
) {}