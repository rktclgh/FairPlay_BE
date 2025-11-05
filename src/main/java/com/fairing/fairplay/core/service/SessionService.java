package com.fairing.fairplay.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 세션 관리 서비스
 * HTTP-only 쿠키와 함께 사용하여 안전한 인증 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String SESSION_PREFIX = "session:";
    private static final String PAYMENT_LOCK_PREFIX = "payment_lock:";
    private static final Duration SESSION_TIMEOUT = Duration.ofDays(7); // 7일 (슬라이딩 세션)
    private static final Duration PAYMENT_LOCK_TIMEOUT = Duration.ofMinutes(15); // 15분

    /**
     * 새 세션 생성 및 사용자 정보 저장
     */
    public String createSession(Long userId, String email, String role, Long roleId) {
        String sessionId = UUID.randomUUID().toString();
        String sessionKey = SESSION_PREFIX + sessionId;

        try {
            Map<String, Object> sessionData = Map.of(
                "userId", userId,
                "email", email,
                "role", role,
                "roleId", roleId,
                "createdAt", System.currentTimeMillis()
            );

            String sessionJson = objectMapper.writeValueAsString(sessionData);
            redisTemplate.opsForValue().set(sessionKey, sessionJson, SESSION_TIMEOUT);

            log.debug("세션 생성 - sessionKey: {}", sessionKey);

            log.debug("세션 생성 완료 - sessionId: {}, userId: {}", sessionId, userId);
            return sessionId;
        } catch (JsonProcessingException e) {
            log.error("세션 생성 실패 - userId: {}", userId, e);
            throw new RuntimeException("세션 생성에 실패했습니다.", e);
        }
    }

    /**
     * 세션 정보 조회
     */
    public Map<String, Object> getSessionData(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return null;
        }

        String sessionKey = SESSION_PREFIX + sessionId;
        String sessionJson = redisTemplate.opsForValue().get(sessionKey);

        if (sessionJson == null) {
            log.warn("세션을 찾을 수 없음 - sessionId: {}", sessionId);
            return null;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> sessionData = objectMapper.readValue(sessionJson, Map.class);
            
            // 세션 TTL 연장 (슬라이딩 세션)
            redisTemplate.expire(sessionKey, SESSION_TIMEOUT);
            
            return sessionData;
        } catch (JsonProcessingException e) {
            log.error("세션 데이터 파싱 실패 - sessionId: {}", sessionId, e);
            return null;
        }
    }

    /**
     * 세션 삭제 (로그아웃)
     */
    public void deleteSession(String sessionId) {
        if (sessionId != null && !sessionId.isEmpty()) {
            String sessionKey = SESSION_PREFIX + sessionId;
            redisTemplate.delete(sessionKey);
            log.debug("세션 삭제 완료 - sessionId: {}", sessionId);
        }
    }

    /**
     * 세션에서 사용자 ID 조회
     */
    public Long getUserIdFromSession(String sessionId) {
        Map<String, Object> sessionData = getSessionData(sessionId);
        if (sessionData != null && sessionData.containsKey("userId")) {
            Object userIdObj = sessionData.get("userId");
            if (userIdObj instanceof Number) {
                return ((Number) userIdObj).longValue();
            }
        }
        return null;
    }

    /**
     * 세션 유효성 검증
     */
    public boolean isValidSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return false;
        }

        String sessionKey = SESSION_PREFIX + sessionId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey));
    }

    /**
     * 사용자의 모든 세션 삭제 (보안용)
     */
    public void deleteAllUserSessions(Long userId) {
        // TODO: 구현 필요 시 사용자별 세션 추적 방식 추가
    }

    /**
     * 사용자의 결제 진행 상태 설정 (사용자 ID 기반)
     */
    public boolean setPaymentLock(Long userId, String merchantUid) {
        if (userId == null || merchantUid == null) {
            return false;
        }
        
        String lockKey = PAYMENT_LOCK_PREFIX + userId;
        
        try {
            // 이미 결제 진행 중인지 확인
            if (isPaymentLocked(userId)) {
                return false; // 이미 결제 진행 중
            }
            
            Map<String, Object> lockData = Map.of(
                "merchantUid", merchantUid,
                "startedAt", System.currentTimeMillis(),
                "userId", userId
            );
            
            String lockJson = objectMapper.writeValueAsString(lockData);
            
            // SET IF NOT EXISTS로 원자적 처리
            Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, lockJson, PAYMENT_LOCK_TIMEOUT);
            
            if (Boolean.TRUE.equals(success)) {
                log.debug("결제 락 설정 성공 - userId: {}, merchantUid: {}", userId, merchantUid);
                return true;
            } else {
                log.warn("결제 락 설정 실패 (이미 존재) - userId: {}", userId);
                return false;
            }
        } catch (JsonProcessingException e) {
            log.error("결제 락 설정 실패 - userId: {}", userId, e);
            return false;
        }
    }

    /**
     * 사용자의 결제 진행 상태 확인 (사용자 ID 기반)
     */
    public boolean isPaymentLocked(Long userId) {
        if (userId == null) {
            return false;
        }
        
        String lockKey = PAYMENT_LOCK_PREFIX + userId;
        String lockJson = redisTemplate.opsForValue().get(lockKey);
        
        if (lockJson == null) {
            return false;
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> lockData = objectMapper.readValue(lockJson, Map.class);
            
            // 시간 기반 자동 만료 체크
            long startedAt = ((Number) lockData.get("startedAt")).longValue();
            long elapsed = System.currentTimeMillis() - startedAt;
            
            if (elapsed > PAYMENT_LOCK_TIMEOUT.toMillis()) {
                // 만료된 락 삭제
                clearPaymentLock(userId);
                log.debug("만료된 결제 락 정리 - userId: {}, elapsed: {}ms", userId, elapsed);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("결제 락 상태 확인 실패 - userId: {}", userId, e);
            // 오류 시 안전을 위해 락 해제
            clearPaymentLock(userId);
            return false;
        }
    }

    /**
     * 사용자의 결제 진행 상태 해제 (사용자 ID 기반)
     */
    public void clearPaymentLock(Long userId) {
        if (userId == null) {
            return;
        }
        
        String lockKey = PAYMENT_LOCK_PREFIX + userId;
        Boolean deleted = redisTemplate.delete(lockKey);
        log.debug("결제 락 해제 - userId: {}, deleted: {}", userId, deleted);
    }

    /**
     * merchantUid로 결제 락 해제 (백업용)
     */
    public void clearPaymentLockByMerchantUid(String merchantUid) {
        if (merchantUid == null) {
            return;
        }
        
        try {
            // 모든 결제 락을 검색해서 해당 merchantUid와 일치하는 것 찾기
            String pattern = PAYMENT_LOCK_PREFIX + "*";
            var keys = redisTemplate.keys(pattern);
            
            if (keys != null) {
                for (String key : keys) {
                    String lockJson = redisTemplate.opsForValue().get(key);
                    if (lockJson != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> lockData = objectMapper.readValue(lockJson, Map.class);
                        String storedMerchantUid = (String) lockData.get("merchantUid");
                        
                        if (merchantUid.equals(storedMerchantUid)) {
                            redisTemplate.delete(key);
                            log.debug("merchantUid로 결제 락 해제 - merchantUid: {}", merchantUid);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("merchantUid로 결제 락 해제 실패 - merchantUid: {}", merchantUid, e);
        }
    }
}