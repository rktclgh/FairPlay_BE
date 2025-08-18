package com.fairing.fairplay.banner.batch;

import com.fairing.fairplay.banner.repository.BannerRepository;
import com.fairing.fairplay.banner.repository.BannerStatusCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;


@Slf4j
@Component
@RequiredArgsConstructor
public class BannerExpirationScheduler {

    private final BannerRepository bannerRepository;
    private final BannerStatusCodeRepository statusRepo;

    // 매일 새벽 1시
    @Scheduled(cron = "0 1 1 * * *")
    @Transactional
    public void deactivateExpiredBanners() {
        var inactive = statusRepo.findByCode("INACTIVE")
                .orElseThrow(() -> new IllegalStateException("INACTIVE 상태 코드 없음"));
        int updated = bannerRepository.deactivateExpiredAll(LocalDateTime.now(), inactive); // 전체 타입 만료 처리
        if (updated > 0) {
            log.info("만료된 배너 {}건을 INACTIVE로 변경", updated);
        } else {
            log.debug("만료된 HERO/SEARCH_TOP 배너 없음");
        }
    }
}