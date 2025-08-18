package com.fairing.fairplay.banner.dto;

import java.util.List;

public record FinalizeSoldResponseDto(
        List<Long> bannerIds     // 생성된 banner_id 목록(슬롯별 1개씩)
) {}