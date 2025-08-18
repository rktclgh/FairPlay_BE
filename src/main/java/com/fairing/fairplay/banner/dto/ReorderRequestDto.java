package com.fairing.fairplay.banner.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;


public record ReorderRequestDto(
        @NotBlank String type,
        @NotNull LocalDate date,
        @NotNull @NotEmpty List<@Valid Item> items
) {
    public record Item(@NotNull Long bannerId, @Positive int priority) {}
}