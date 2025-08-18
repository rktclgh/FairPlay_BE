package com.fairing.fairplay.banner.dto;

import java.time.LocalDate;
import java.util.List;


public record ReorderRequestDto(String type, LocalDate date, List<Item> items) {
    public record Item(Long bannerId, int priority) {}
}