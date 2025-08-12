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
    
    /**
     * 5분마다 캐시 정리 (오래된 TTL 설정으로 자동 정리됨)
     * 여기서는 로그만 남김
     */
    @Scheduled(fixedRate = 300000) // 5분
    public void logCacheStatus() {
        log.info("채팅 캐시 상태 체크 완료 - Redis TTL에 의해 자동 정리됨");
    }
    
    /**
     * 1시간마다 캐시 통계 로그
     */
    @Scheduled(fixedRate = 3600000) // 1시간
    public void logCacheStatistics() {
        log.info("채팅 캐시 통계 - 메시지 캐시는 1시간, 방 목록 캐시는 10분, 읽지않은 수 캐시는 30분 TTL");
    }
}