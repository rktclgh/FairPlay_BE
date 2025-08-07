package com.fairing.fairplay.chat.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatPresenceService {

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
}
