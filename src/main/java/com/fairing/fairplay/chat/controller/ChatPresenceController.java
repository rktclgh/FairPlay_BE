package com.fairing.fairplay.chat.controller;

import com.fairing.fairplay.chat.service.ChatPresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
}
