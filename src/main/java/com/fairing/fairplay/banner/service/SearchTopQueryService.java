package com.fairing.fairplay.banner.service;

import com.fairing.fairplay.banner.dto.FixedTopDto;
import com.fairing.fairplay.banner.repository.BannerSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchTopQueryService {
    private final BannerSlotRepository slotRepo;

    public List<FixedTopDto> getFixedTwo(LocalDate date) {
        return slotRepo.findSoldFixedRows("SEARCH_TOP", date)
                .stream()
                .limit(2) // 두 칸만
                .map(r -> new FixedTopDto(r.getEventId(), r.getPriority(), true))
                .toList();
    }
    
    public List<FixedTopDto> getHeroBanners(LocalDate date) {
        return slotRepo.findActiveHeroBanners("HERO", date)
                .stream()
                .map(r -> new FixedTopDto(r.getEventId(), r.getPriority(), true))
                .toList();
    }
}
