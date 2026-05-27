package com.fairing.fairplay.core.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
    private static final String USER_SESSIONS_PREFIX = "user_sessions:";
    private static final String USER_SESSIONS_REVOKED_AT_PREFIX = "user_sessions_revoked_at:";
    private static final String USER_SESSIONS_BLOCKED_PREFIX = "user_sessions_blocked:";
    private static final String PAYMENT_LOCK_PREFIX = "payment_lock:";
    private static final int CURRENT_SESSION_VERSION = 2;
    private static final Duration SESSION_TIMEOUT = Duration.ofDays(7); // 7일 (슬라이딩 세션)
    private static final Duration PAYMENT_LOCK_TIMEOUT = Duration.ofMinutes(15); // 15분

    /**
     * 새 세션 생성 및 사용자 정보 저장
     */
    public String createSession(Long userId, String email, String role, Long roleId) {
        return createSession(userId, email, role, roleId, System.currentTimeMillis());
    }

    public String createSession(Long userId, String email, String role, Long roleId, Long authenticatedAt) {
        String sessionId = UUID.randomUUID().toString();
        String sessionKey = SESSION_PREFIX + sessionId;
        long createdAt = authenticatedAt != null ? authenticatedAt : System.currentTimeMillis();
        assertSessionCanBeIssued(userId, createdAt);

        try {
            Map<String, Object> sessionData = Map.of(
                "userId", userId,
                "email", email,
                "role", role,
                "roleId", roleId,
                "createdAt", createdAt,
                "sessionVersion", CURRENT_SESSION_VERSION
            );

            String sessionJson = objectMapper.writeValueAsString(sessionData);
            redisTemplate.opsForValue().set(sessionKey, sessionJson, SESSION_TIMEOUT);
            addUserSession(userId, sessionId);

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

            if (!isCurrentSessionVersion(sessionData)) {
                deleteSession(sessionId);
                log.debug("구버전 세션 차단 - sessionId: {}", sessionId);
                return null;
            }

            Long userId = getLongValue(sessionData.get("userId"));
            Long createdAt = getLongValue(sessionData.get("createdAt"));
            if (isUserSessionsBlocked(userId)) {
                log.debug("사용자 세션 차단 중 - sessionId: {}, userId: {}", sessionId, userId);
                return null;
            }
            if (isRevokedUserSession(userId, createdAt)) {
                deleteSession(sessionId);
                log.debug("폐기된 사용자 세션 차단 - sessionId: {}, userId: {}", sessionId, userId);
                return null;
            }
            
            // 세션 TTL 연장 (슬라이딩 세션)
            redisTemplate.expire(sessionKey, SESSION_TIMEOUT);
            if (userId != null) {
                addUserSession(userId, sessionId);
            }
            
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
            removeSessionFromUserIndex(sessionId, redisTemplate.opsForValue().get(sessionKey));
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
        return getSessionData(sessionId) != null;
    }

    /**
     * 사용자의 모든 세션 삭제 (보안용)
     */
    public void deleteAllUserSessions(Long userId) {
        if (userId == null) {
            return;
        }

        String userSessionsKey = userSessionsKey(userId);
        String revokedAtKey = USER_SESSIONS_REVOKED_AT_PREFIX + userId;
        redisTemplate.opsForValue().set(
                revokedAtKey,
                String.valueOf(System.currentTimeMillis()),
                SESSION_TIMEOUT
        );

        Set<String> sessionIds = redisTemplate.opsForSet().members(userSessionsKey);
        if (sessionIds == null || sessionIds.isEmpty()) {
            redisTemplate.delete(userSessionsKey);
            log.debug("사용자 세션 폐기 마커 설정 - userId: {}, indexedSessions: 0", userId);
            return;
        }

        List<String> sessionKeys = new ArrayList<>();
        for (String sessionId : sessionIds) {
            sessionKeys.add(SESSION_PREFIX + sessionId);
        }

        redisTemplate.delete(sessionKeys);
        redisTemplate.delete(userSessionsKey);
        log.debug("사용자 전체 세션 삭제 완료 - userId: {}, deletedSessions: {}", userId, sessionIds.size());
    }

    public void assertSessionCanBeIssued(Long userId, Long authenticatedAt) {
        long issuedAt = authenticatedAt != null ? authenticatedAt : System.currentTimeMillis();
        if (isUserSessionsBlocked(userId)) {
            throw new CustomException(HttpStatus.CONFLICT, "계정 상태 변경이 진행 중입니다. 잠시 후 다시 로그인해주세요.");
        }
        if (isRevokedUserSession(userId, issuedAt)) {
            throw new CustomException(HttpStatus.CONFLICT, "계정 상태가 변경되었습니다. 다시 로그인해주세요.");
        }
    }

    public void blockUserSessions(Long userId) {
        if (userId == null) {
            return;
        }

        redisTemplate.opsForValue().set(
                userSessionsBlockedKey(userId),
                String.valueOf(System.currentTimeMillis()),
                SESSION_TIMEOUT
        );
        log.debug("사용자 세션 임시 차단 설정 - userId: {}", userId);
    }

    public void unblockUserSessions(Long userId) {
        if (userId == null) {
            return;
        }

        redisTemplate.delete(userSessionsBlockedKey(userId));
        log.debug("사용자 세션 임시 차단 해제 - userId: {}", userId);
    }

    private void addUserSession(Long userId, String sessionId) {
        if (userId == null || sessionId == null || sessionId.isBlank()) {
            return;
        }

        String userSessionsKey = userSessionsKey(userId);
        redisTemplate.opsForSet().add(userSessionsKey, sessionId);
        redisTemplate.expire(userSessionsKey, SESSION_TIMEOUT);
    }

    private void removeSessionFromUserIndex(String sessionId, String sessionJson) {
        Long userId = extractUserId(sessionJson);
        if (userId == null) {
            return;
        }

        String userSessionsKey = userSessionsKey(userId);
        redisTemplate.opsForSet().remove(userSessionsKey, sessionId);
        Long remaining = redisTemplate.opsForSet().size(userSessionsKey);
        if (remaining != null && remaining == 0) {
            redisTemplate.delete(userSessionsKey);
        }
    }

    private Long extractUserId(String sessionJson) {
        if (sessionJson == null) {
            return null;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> sessionData = objectMapper.readValue(sessionJson, Map.class);
            return getLongValue(sessionData.get("userId"));
        } catch (JsonProcessingException e) {
            log.warn("세션 사용자 인덱스 정리 중 데이터 파싱 실패", e);
            return null;
        }
    }

    private boolean isRevokedUserSession(Long userId, Long createdAt) {
        if (userId == null) {
            return false;
        }

        String revokedAt = redisTemplate.opsForValue().get(USER_SESSIONS_REVOKED_AT_PREFIX + userId);
        if (revokedAt == null) {
            return false;
        }
        if (createdAt == null) {
            return true;
        }

        try {
            return createdAt <= Long.parseLong(revokedAt);
        } catch (NumberFormatException e) {
            log.warn("사용자 세션 폐기 마커 파싱 실패 - userId: {}, revokedAt: {}", userId, revokedAt);
            return true;
        }
    }

    private Long getLongValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String string && !string.isBlank()) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String userSessionsKey(Long userId) {
        return USER_SESSIONS_PREFIX + userId;
    }

    String userSessionsRevokedAtKey(Long userId) {
        return USER_SESSIONS_REVOKED_AT_PREFIX + userId;
    }

    private boolean isCurrentSessionVersion(Map<String, Object> sessionData) {
        Long sessionVersion = getLongValue(sessionData.get("sessionVersion"));
        return sessionVersion != null && sessionVersion == CURRENT_SESSION_VERSION;
    }

    private boolean isUserSessionsBlocked(Long userId) {
        return userId != null && Boolean.TRUE.equals(redisTemplate.hasKey(userSessionsBlockedKey(userId)));
    }

    String userSessionsBlockedKey(Long userId) {
        return USER_SESSIONS_BLOCKED_PREFIX + userId;
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
