package com.fairing.fairplay.banner.batch;

import com.fairing.fairplay.banner.repository.BannerSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;

@Transactional
@Slf4j
@Component
@RequiredArgsConstructor
public class BannerSlotLockCleanup {

    private final BannerSlotRepository bannerSlotRepository;

    // 5분마다 만료 락 해제
    @Scheduled(fixedDelayString = "${banner.lock.cleanup.delay:300000}")
    public void releaseExpiredLocks() {
        int n = bannerSlotRepository.releaseExpiredLocks();
        if (n > 0) log.info("Released {} expired banner slot locks", n);
    }
}