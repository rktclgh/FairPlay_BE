package com.fairing.fairplay.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 사용자 온라인 상태 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserPresenceService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    // Redis 키 패턴
    private static final String USER_ONLINE_KEY = "user:online:%d";
    private static final String ONLINE_USERS_SET = "users:online";
    private static final String USER_LAST_SEEN_KEY = "user:lastseen:%d";
    
    // 온라인 상태 유지 시간 (5분)
    private static final Duration ONLINE_DURATION = Duration.ofMinutes(5);
    
    /**
     * 사용자를 온라인 상태로 설정
     */
    public void setUserOnline(Long userId) {
        try {
            String userKey = String.format(USER_ONLINE_KEY, userId);
            String lastSeenKey = String.format(USER_LAST_SEEN_KEY, userId);
            String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            // 개별 사용자 온라인 상태 (TTL 5분)
            redisTemplate.opsForValue().set(userKey, now, ONLINE_DURATION);
            
            // 온라인 사용자 집합에 추가 (TTL 5분)
            redisTemplate.opsForSet().add(ONLINE_USERS_SET, userId.toString());
            redisTemplate.expire(ONLINE_USERS_SET, ONLINE_DURATION);
            
            // 마지막 접속 시간 업데이트 (TTL 24시간)
            redisTemplate.opsForValue().set(lastSeenKey, now, Duration.ofHours(24));
            
            log.debug("사용자 온라인 상태 설정: userId={}", userId);
        } catch (Exception e) {
            log.error("사용자 온라인 상태 설정 실패: userId={}, error={}", userId, e.getMessage());
        }
    }
    
    /**
     * 사용자 온라인 상태 확인
     */
    public boolean isUserOnline(Long userId) {
        try {
            String userKey = String.format(USER_ONLINE_KEY, userId);
            return redisTemplate.hasKey(userKey);
        } catch (Exception e) {
            log.error("사용자 온라인 상태 확인 실패: userId={}, error={}", userId, e.getMessage());
            return false;
        }
    }
    
    /**
     * 사용자를 오프라인 상태로 설정
     */
    public void setUserOffline(Long userId) {
        try {
            String userKey = String.format(USER_ONLINE_KEY, userId);
            String lastSeenKey = String.format(USER_LAST_SEEN_KEY, userId);
            String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            // 온라인 상태 제거
            redisTemplate.delete(userKey);
            
            // 온라인 사용자 집합에서 제거
            redisTemplate.opsForSet().remove(ONLINE_USERS_SET, userId.toString());
            
            // 마지막 접속 시간 업데이트
            redisTemplate.opsForValue().set(lastSeenKey, now, Duration.ofHours(24));
            
            log.debug("사용자 오프라인 상태 설정: userId={}", userId);
        } catch (Exception e) {
            log.error("사용자 오프라인 상태 설정 실패: userId={}, error={}", userId, e.getMessage());
        }
    }
    
    /**
     * 사용자의 온라인 상태 갱신 (heartbeat)
     */
    public void refreshUserPresence(Long userId) {
        if (isUserOnline(userId)) {
            setUserOnline(userId); // TTL 재설정
        }
    }
    
    /**
     * 모든 온라인 사용자 수 조회
     */
    public long getOnlineUserCount() {
        try {
            Long count = redisTemplate.opsForSet().size(ONLINE_USERS_SET);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("온라인 사용자 수 조회 실패: error={}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * 모든 온라인 사용자 ID 목록 조회
     */
    public Set<String> getOnlineUserIds() {
        try {
            return redisTemplate.opsForSet().members(ONLINE_USERS_SET);
        } catch (Exception e) {
            log.error("온라인 사용자 목록 조회 실패: error={}", e.getMessage());
            return Set.of();
        }
    }
    
    /**
     * 사용자의 마지막 접속 시간 조회
     */
    public String getUserLastSeen(Long userId) {
        try {
            String lastSeenKey = String.format(USER_LAST_SEEN_KEY, userId);
            return redisTemplate.opsForValue().get(lastSeenKey);
        } catch (Exception e) {
            log.error("마지막 접속 시간 조회 실패: userId={}, error={}", userId, e.getMessage());
            return null;
        }
    }
    
    /**
     * 사용자의 온라인 상태 정보 조회 (온라인 여부 + 마지막 접속 시간)
     */
    public UserPresenceInfo getUserPresenceInfo(Long userId) {
        boolean isOnline = isUserOnline(userId);
        String lastSeen = getUserLastSeen(userId);
        
        return UserPresenceInfo.builder()
                .userId(userId)
                .isOnline(isOnline)
                .lastSeen(lastSeen)
                .build();
    }
    
    /**
     * 만료된 온라인 상태 정리 (스케줄러에서 호출)
     */
    public void cleanupExpiredPresence() {
        try {
            Set<String> onlineUsers = getOnlineUserIds();
            int cleanedCount = 0;
            
            for (String userIdStr : onlineUsers) {
                try {
                    Long userId = Long.parseLong(userIdStr);
                    if (!isUserOnline(userId)) {
                        redisTemplate.opsForSet().remove(ONLINE_USERS_SET, userIdStr);
                        cleanedCount++;
                    }
                } catch (NumberFormatException e) {
                    // 잘못된 형식의 사용자 ID 제거
                    redisTemplate.opsForSet().remove(ONLINE_USERS_SET, userIdStr);
                    cleanedCount++;
                }
            }
            
            if (cleanedCount > 0) {
                log.info("만료된 온라인 상태 정리 완료: {} 개 사용자", cleanedCount);
            }
        } catch (Exception e) {
            log.error("만료된 온라인 상태 정리 실패: error={}", e.getMessage());
        }
    }
    
    /**
     * 사용자 온라인 상태 정보 DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class UserPresenceInfo {
        private Long userId;
        private boolean isOnline;
        private String lastSeen;
    }
}