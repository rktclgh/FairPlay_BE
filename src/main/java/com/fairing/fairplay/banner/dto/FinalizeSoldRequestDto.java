package com.fairing.fairplay.banner.dto;

import java.util.List;

public record FinalizeSoldRequestDto(
        List<Long> slotIds,      // LOCK 상태였던 슬롯들
        Long eventId,            // 고정 노출시킬 이벤트
        String title,            // 배너 타이틀(선택)
        String imageUrl,
        String linkUrl
) {}
