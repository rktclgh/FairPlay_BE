package com.fairing.fairplay.banner.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;
import java.util.List;

public record CreateApplicationRequestDto(
        @NotNull(message = "이벤트 ID는 필수입니다")
        Long eventId,

        @NotBlank(message = "배너 타입 코드는 필수입니다")
        @Pattern(regexp = "^(HERO|SEARCH_TOP)$", message = "배너 타입 코드는 HERO 또는 SEARCH_TOP이어야 합니다")
        String bannerTypeCode,        // "HERO" | "SEARCH_TOP"

        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다")
        String title,

        @NotBlank(message = "이미지 URL은 필수입니다")
        @URL(message = "올바른 이미지 URL 형식이어야 합니다")
        String imageUrl,

        @URL(message = "올바른 링크 URL 형식이어야 합니다")
        String linkUrl,

        @NotEmpty(message = "아이템 목록은 비어있을 수 없습니다")
        @Valid
        List<Item> items,             // 날짜/순위 묶음

        @Min(value = 1, message = "잠금 시간은 1분 이상이어야 합니다")
        Integer lockMinutes           // null이면 기본 2880(=48h)
) {
    public record Item(
            @NotNull(message = "날짜는 필수입니다")
            @FutureOrPresent(message = "날짜는 현재 또는 미래여야 합니다")
            LocalDate date,
            @NotNull(message = "우선순위는 필수입니다")
            @Min(value = 1, message = "우선순위는 1 이상이어야 합니다")
            Integer priority) {}
}