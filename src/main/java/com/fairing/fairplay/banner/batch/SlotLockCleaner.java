package com.fairing.fairplay.banner.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SlotLockCleaner {

    private final JdbcTemplate jdbc;

    @Scheduled(fixedDelay = 10 * 60 * 1000) // 10분마다
    @Transactional
    public void releaseExpiredLocks() {
        jdbc.update("""
            UPDATE banner_slot
            SET status='AVAILABLE', locked_by=NULL, locked_until=NULL
            WHERE status='LOCKED' AND locked_until < NOW()
        """);
    }
}
