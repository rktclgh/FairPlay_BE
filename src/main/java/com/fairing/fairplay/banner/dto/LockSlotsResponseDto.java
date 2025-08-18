package com.fairing.fairplay.banner.dto;

import java.time.LocalDateTime;
import java.util.List;

public record LockSlotsResponseDto(
        List<Long> slotIds,          // 잠금된 slot_id들
        Integer totalAmount,         // 가격 합계(슬롯 price 합)
        LocalDateTime lockedUntil    // 만료 시각
) {}