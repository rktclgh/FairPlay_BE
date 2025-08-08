package com.fairing.fairplay.chat.controller;

import com.fairing.fairplay.chat.service.ChatPresenceService;
import com.fairing.fairplay.core.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/chat/presence")
@RequiredArgsConstructor
public class ChatPresenceController {

    private final ChatPresenceService chatPresenceService;

    // 기존 메서드들 (호환성 유지)
    @GetMapping
    public boolean isOnline(@RequestParam boolean isManager, @RequestParam Long userId) {
        return chatPresenceService.isOnline(isManager, userId);
    }
    
    @PostMapping("/online")
    public void setOnline(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        boolean isManager = Boolean.parseBoolean(request.get("isManager").toString());
        chatPresenceService.setOnline(isManager, userId);
        System.out.println("채팅방 열기 - 사용자 " + userId + " 온라인 상태로 설정");
    }
    
    @PostMapping("/offline")
    public void setOffline(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        boolean isManager = Boolean.parseBoolean(request.get("isManager").toString());
        chatPresenceService.setOffline(isManager, userId);
        System.out.println("브라우저 닫기 - 사용자 " + userId + " 오프라인 상태로 설정");
    }

    // 새로운 JWT 기반 메서드들
    @PostMapping("/connect")
    public void userConnect(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        chatPresenceService.setUserOnline(userId);
    }

    @PostMapping("/disconnect")
    public void userDisconnect(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        chatPresenceService.setUserOffline(userId);
    }

    @GetMapping("/status/{userId}")
    public Map<String, Object> getUserStatus(@PathVariable Long userId) {
        boolean isOnline = chatPresenceService.isUserOnline(userId);
        return Map.of(
            "userId", userId,
            "isOnline", isOnline
        );
    }

    @GetMapping("/online-users")
    public Set<Long> getOnlineUsers() {
        return chatPresenceService.getOnlineUsers();
    }

    // ADMIN 권한 사용자들의 온라인 상태 확인
    @GetMapping("/admin-status")
    public Map<String, Object> getAdminStatus() {
        Set<Long> onlineAdmins = chatPresenceService.getOnlineAdmins();
        boolean hasOnlineAdmin = !onlineAdmins.isEmpty();
        return Map.of(
            "hasOnlineAdmin", hasOnlineAdmin,
            "onlineAdmins", onlineAdmins,
            "adminCount", onlineAdmins.size()
        );
    }
}
