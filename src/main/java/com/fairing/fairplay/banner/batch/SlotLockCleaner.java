package com.fairing.fairplay.banner.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// ⚠️ DEPRECATED: 이 클래스는 BannerSlotLockCleanup과 중복되어 비활성화되었습니다.
// BannerSlotLockCleanup를 사용하세요 (JPA 기반, 5분마다 실행, 타임아웃 설정 포함)
// @Component  // 비활성화 - 커넥션 풀 고갈 문제 방지
@RequiredArgsConstructor
public class SlotLockCleaner {

    private final JdbcTemplate jdbc;

    // @Scheduled(initialDelay = 60_000, fixedDelay = 10 * 60 * 1000) // 비활성화
    @Transactional(timeout = 10)
    public void releaseExpiredLocks() {
        jdbc.update("""
           UPDATE banner_slot
           SET status='AVAILABLE', locked_by=NULL, locked_until=NULL
           WHERE status='LOCKED' AND locked_until < CURRENT_TIMESTAMP
       """);
    }
}
