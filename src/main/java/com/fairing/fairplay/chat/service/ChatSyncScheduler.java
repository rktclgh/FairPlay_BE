package com.fairing.fairplay.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Redis 캐시와 DB 동기화를 담당하는 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSyncScheduler {
    
    private final ChatCacheService chatCacheService;
    private final UserPresenceService userPresenceService;
    
    /**
     * 2분마다 만료된 온라인 상태 정리
     */
    @Scheduled(fixedRate = 120000) // 2분
    public void cleanupExpiredPresence() {
        userPresenceService.cleanupExpiredPresence();
    }
    
    /**
     * 5분마다 캐시 정리 및 온라인 사용자 통계
     */
    @Scheduled(fixedRate = 300000) // 5분
    public void logCacheStatus() {
        long onlineUserCount = userPresenceService.getOnlineUserCount();
        log.info("채팅 시스템 상태 - 온라인 사용자: {}명, Redis TTL에 의해 자동 정리됨", onlineUserCount);
    }
    
    /**
     * 1시간마다 캐시 통계 로그
     */
    @Scheduled(fixedRate = 3600000) // 1시간
    public void logCacheStatistics() {
        long onlineUsers = userPresenceService.getOnlineUserCount();
        log.info("채팅 캐시 통계 - 온라인 사용자: {}명, 메시지 캐시: 1시간 TTL, 방 목록 캐시: 10분 TTL, 읽지않은 수 캐시: 30분 TTL, 온라인 상태: 5분 TTL", onlineUsers);
    }
}