package com.fairing.fairplay.banner.dto;

import java.time.LocalDate;

public record AdminApplicationSlotDto(
        LocalDate slotDate,
        Integer priority,
        Integer price
) {}
