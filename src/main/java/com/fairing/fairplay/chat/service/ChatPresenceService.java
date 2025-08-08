package com.fairing.fairplay.chat.service;

import com.fairing.fairplay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatPresenceService {

    private final UserRepository userRepository;

    // 간단한 메모리 기반 온라인 상태 관리
    private final ConcurrentHashMap<String, LocalDateTime> onlineUsers = new ConcurrentHashMap<>();
    private static final long ONLINE_EXPIRE_MINUTES = 10; // 10분

    private String getKey(boolean isManager, Long userId) {
        return (isManager ? "manager:" : "user:") + userId;
    }

    public void setOnline(boolean isManager, Long userId) {
        String key = getKey(isManager, userId);
        onlineUsers.put(key, LocalDateTime.now());
        System.out.println("온라인 상태 설정: " + key + " at " + LocalDateTime.now());
    }

    public void setOffline(boolean isManager, Long userId) {
        String key = getKey(isManager, userId);
        onlineUsers.remove(key);
        System.out.println("오프라인 상태 설정: " + key);
    }

    public boolean isOnline(boolean isManager, Long userId) {
        String key = getKey(isManager, userId);
        LocalDateTime lastSeen = onlineUsers.get(key);
        
        if (lastSeen == null) {
            System.out.println("온라인 상태 확인: " + key + " = false (기록 없음)");
            return false;
        }
        
        // 10분 이상 지나면 오프라인으로 간주
        boolean online = lastSeen.isAfter(LocalDateTime.now().minusMinutes(ONLINE_EXPIRE_MINUTES));
        System.out.println("온라인 상태 확인: " + key + " = " + online + " (마지막 접속: " + lastSeen + ")");
        
        return online;
    }

    // 새로운 메서드들 (JWT 기반)
    public void setUserOnline(Long userId) {
        String key = "user:" + userId;
        onlineUsers.put(key, LocalDateTime.now());
        System.out.println("사용자 온라인 설정: " + userId);
    }

    public void setUserOffline(Long userId) {
        String key = "user:" + userId;
        onlineUsers.remove(key);
        System.out.println("사용자 오프라인 설정: " + userId);
    }

    public boolean isUserOnline(Long userId) {
        String key = "user:" + userId;
        LocalDateTime lastSeen = onlineUsers.get(key);
        
        if (lastSeen == null) return false;
        
        return lastSeen.isAfter(LocalDateTime.now().minusMinutes(ONLINE_EXPIRE_MINUTES));
    }

    public Set<Long> getOnlineUsers() {
        return onlineUsers.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("user:"))
                .filter(entry -> entry.getValue().isAfter(LocalDateTime.now().minusMinutes(ONLINE_EXPIRE_MINUTES)))
                .map(entry -> Long.parseLong(entry.getKey().substring(5))) // "user:" 제거
                .collect(Collectors.toSet());
    }

    public Set<Long> getOnlineAdmins() {
        // 온라인 사용자 중 ADMIN 권한을 가진 사용자들만 필터링
        Set<Long> onlineUserIds = getOnlineUsers();
        
        return userRepository.findByUserIdInAndRoleCode_Code(onlineUserIds, "ADMIN")
                .stream()
                .map(user -> user.getUserId())
                .collect(Collectors.toSet());
    }
}
