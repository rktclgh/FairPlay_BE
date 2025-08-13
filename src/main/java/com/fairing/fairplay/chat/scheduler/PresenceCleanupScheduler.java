package com.fairing.fairplay.chat.scheduler;

import com.fairing.fairplay.chat.service.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 사용자 온라인 상태의 만료된 데이터를 정리하는 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PresenceCleanupScheduler {
    
    private final UserPresenceService userPresenceService;
    
    /**
     * 매 5분마다 만료된 온라인 상태 데이터 정리
     */
    @Scheduled(fixedRate = 300000) // 5분 = 300,000ms
    public void cleanupExpiredPresence() {
        try {
            log.debug("만료된 온라인 상태 정리 작업 시작");
            userPresenceService.cleanupExpiredPresence();
            log.debug("만료된 온라인 상태 정리 작업 완료");
        } catch (Exception e) {
            log.error("만료된 온라인 상태 정리 작업 실패", e);
        }
    }
    
    /**
     * 매 1시간마다 온라인 사용자 통계 로깅
     */
    @Scheduled(fixedRate = 3600000) // 1시간 = 3,600,000ms  
    public void logOnlineUserStats() {
        try {
            long onlineCount = userPresenceService.getOnlineUserCount();
            log.info("현재 온라인 사용자 수: {}", onlineCount);
        } catch (Exception e) {
            log.error("온라인 사용자 통계 로깅 실패", e);
        }
    }
}