package com.fairing.fairplay.booth.service;

import com.fairing.fairplay.booth.dto.*;

import java.util.List;

public interface BoothApplicationService {
    Long applyBooth(BoothApplicationRequestDto dto);

    List<BoothApplicationListDto> getBoothApplications(Long eventId);

    BoothApplicationResponseDto getBoothApplication(Long id);

    void updateStatus(Long id, BoothApplicationStatusUpdateDto dto);
}
