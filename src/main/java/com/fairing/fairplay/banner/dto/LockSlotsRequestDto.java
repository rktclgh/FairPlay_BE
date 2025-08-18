package com.fairing.fairplay.banner.dto;


import java.time.LocalDate;
import java.util.List;

public record LockSlotsRequestDto(
        String typeCode,             // 'SEARCH_TOP' 또는 'HERO'
        List<Item> items,            // 잠글 (날짜, 우선순위) 목록
        Integer holdHours            // null이면 48시간
) {
    public record Item(LocalDate slotDate, Integer priority) {}
}