package com.fairing.fairplay.chat.controller;

import com.fairing.fairplay.chat.service.ChatPresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat/presence")
@RequiredArgsConstructor
public class ChatPresenceController {

    private final ChatPresenceService chatPresenceService;

    // 유저/관리자 온라인 상태 조회
    @GetMapping
    public boolean isOnline(@RequestParam boolean isManager, @RequestParam Long userId) {
        return chatPresenceService.isOnline(isManager, userId);
    }
    
    // 온라인 상태 설정 (채팅방 열었을 때 사용)
    @PostMapping("/online")
    public void setOnline(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        boolean isManager = Boolean.parseBoolean(request.get("isManager").toString());
        chatPresenceService.setOnline(isManager, userId);
        System.out.println("채팅방 열기 - 사용자 " + userId + " 온라인 상태로 설정");
    }
    
    // 오프라인 상태 설정 (브라우저 닫을 때 사용)
    @PostMapping("/offline")
    public void setOffline(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        boolean isManager = Boolean.parseBoolean(request.get("isManager").toString());
        chatPresenceService.setOffline(isManager, userId);
        System.out.println("브라우저 닫기 - 사용자 " + userId + " 오프라인 상태로 설정");
    }
}
