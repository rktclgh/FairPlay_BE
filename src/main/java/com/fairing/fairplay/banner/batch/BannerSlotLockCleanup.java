package com.fairing.fairplay.banner.batch;

import com.fairing.fairplay.banner.repository.BannerSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Slf4j
@Component
@RequiredArgsConstructor
public class BannerSlotLockCleanup {

    private final BannerSlotRepository bannerSlotRepository;

    // 5분마다 만료 락 해제
    @Scheduled(fixedDelayString = "${banner.lock.cleanup.delay:300000}")
    public void releaseExpiredLocks() {
        try {
            int n = bannerSlotRepository.releaseExpiredLocks();
            if (n > 0) {
                log.info("Released {} expired banner slot locks", n);
            } else {
                log.debug("No expired banner slot locks to release");
            }
        } catch (Exception e) {
            log.warn("Failed to release expired banner slot locks", e);
        }
    }
}