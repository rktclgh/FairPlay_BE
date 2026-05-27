package com.fairing.fairplay.banner.service;

import com.fairing.fairplay.banner.repository.BannerActionCodeRepository;
import com.fairing.fairplay.banner.repository.BannerLogRepository;
import com.fairing.fairplay.banner.repository.BannerRepository;
import com.fairing.fairplay.banner.repository.BannerSlotRepository;
import com.fairing.fairplay.banner.repository.BannerStatusCodeRepository;
import com.fairing.fairplay.banner.repository.BannerTypeRepository;
import com.fairing.fairplay.core.service.LocalFileService;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.file.service.FileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BannerServicePostgresQueryTest {

    private static final LocalDateTime VIP_SEARCH_MIN_DATE = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime VIP_SEARCH_MAX_DATE = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    @Mock
    private BannerRepository bannerRepository;
    @Mock
    private BannerStatusCodeRepository bannerStatusCodeRepository;
    @Mock
    private BannerActionCodeRepository bannerActionCodeRepository;
    @Mock
    private BannerLogRepository bannerLogRepository;
    @Mock
    private BannerSlotRepository bannerSlotRepository;
    @Mock
    private FileService fileService;
    @Mock
    private LocalFileService localFileService;
    @Mock
    private BannerTypeRepository bannerTypeRepository;
    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private BannerService bannerService;

    @Test
    void vipSearchWithoutKeywordUsesQueryWithoutLikePredicate() {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now().plusDays(1);
        when(bannerRepository.search("HERO", null, from, to)).thenReturn(List.of());

        bannerService.searchVip("HERO", null, "   ", from, to);

        verify(bannerRepository).search("HERO", null, from, to);
        verify(bannerRepository, never()).searchByEventTitle("HERO", null, from, to, "   ");
    }

    @Test
    void vipSearchWithKeywordUsesTitleSearchQuery() {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now().plusDays(1);
        when(bannerRepository.searchByEventTitle("HERO", "ACTIVE", from, to, "GD")).thenReturn(List.of());

        bannerService.searchVip("HERO", "ACTIVE", "GD", from, to);

        verify(bannerRepository).searchByEventTitle("HERO", "ACTIVE", from, to, "GD");
        verify(bannerRepository, never()).search("HERO", "ACTIVE", from, to);
    }

    @Test
    void vipSearchWithoutDateFiltersUsesTypedBoundsForPostgres() {
        when(bannerRepository.search("HERO", null, VIP_SEARCH_MIN_DATE, VIP_SEARCH_MAX_DATE)).thenReturn(List.of());

        bannerService.searchVip("HERO", null, null, null, null);

        verify(bannerRepository).search("HERO", null, VIP_SEARCH_MIN_DATE, VIP_SEARCH_MAX_DATE);
    }
}
