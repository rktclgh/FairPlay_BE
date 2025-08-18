package com.fairing.fairplay.banner.dto;

public record FixedTopDto(
        Long eventId,
        Integer priority,
        boolean mdPick
) {}