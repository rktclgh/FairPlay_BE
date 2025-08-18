package com.fairing.fairplay.booth.service;

import com.fairing.fairplay.booth.dto.BoothUpdateRequestDto;

public interface BoothService {
    void updateBooth(Long boothId, BoothUpdateRequestDto dto);
}
