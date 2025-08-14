package com.fairing.fairplay.chat.service;

import com.fairing.fairplay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatPresenceService {

    private final UserRepository userRepository;
    private final UserPresenceService userPresenceService;

    // 기존 호환성 메서드들 (Redis로 위임)
    public void setOnline(boolean isManager, Long userId) {
        userPresenceService.setUserOnline(userId);
        System.out.println("온라인 상태 설정 (Redis): userId=" + userId + ", isManager=" + isManager);
    }

    public void setOffline(boolean isManager, Long userId) {
        userPresenceService.setUserOffline(userId);
        System.out.println("오프라인 상태 설정 (Redis): userId=" + userId + ", isManager=" + isManager);
    }

    public boolean isOnline(boolean isManager, Long userId) {
        boolean online = userPresenceService.isUserOnline(userId);
        System.out.println("온라인 상태 확인 (Redis): userId=" + userId + ", isManager=" + isManager + ", online=" + online);
        return online;
    }

    // 새로운 메서드들 (Redis 기반)
    public void setUserOnline(Long userId) {
        userPresenceService.setUserOnline(userId);
        System.out.println("사용자 온라인 설정 (Redis): " + userId);
    }

    public void setUserOffline(Long userId) {
        userPresenceService.setUserOffline(userId);
        System.out.println("사용자 오프라인 설정 (Redis): " + userId);
    }

    public boolean isUserOnline(Long userId) {
        return userPresenceService.isUserOnline(userId);
    }

    public Set<Long> getOnlineUsers() {
        Set<String> onlineUserIds = userPresenceService.getOnlineUserIds();
        return onlineUserIds.stream()
                .map(idStr -> {
                    try {
                        return Long.parseLong(idStr);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(id -> id != null)
                .collect(Collectors.toSet());
    }

    public Set<Long> getOnlineAdmins() {
        // 온라인 사용자 중 ADMIN 권한을 가진 사용자들만 필터링
        Set<Long> onlineUserIds = getOnlineUsers();
        
        if (onlineUserIds.isEmpty()) {
            return Set.of();
        }
        
        return userRepository.findByUserIdInAndRoleCode_Code(onlineUserIds, "ADMIN")
                .stream()
                .map(user -> user.getUserId())
                .collect(Collectors.toSet());
    }
}
