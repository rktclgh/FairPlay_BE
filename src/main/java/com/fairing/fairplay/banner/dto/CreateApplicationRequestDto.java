package com.fairing.fairplay.banner.dto;

import java.time.LocalDate;
import java.util.List;

public record CreateApplicationRequestDto(
        Long eventId,
        String bannerTypeCode,        // "HERO" | "SEARCH_TOP"
        String title,
        String imageUrl,
        String linkUrl,
        List<Item> items,             // 날짜/순위 묶음
        Integer lockMinutes           // null이면 기본 2880(=48h)
) {
    public record Item(LocalDate date, Integer priority) {}
}