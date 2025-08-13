package com.fairing.fairplay.ai.controller;

import com.fairing.fairplay.ai.dto.ChatRequestDto;
import com.fairing.fairplay.ai.dto.ChatResponseDto;
import com.fairing.fairplay.ai.service.ChatAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class ChatAiController {

    private final ChatAiService chatAiService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponseDto> chat(@RequestBody ChatRequestDto req) throws Exception {
        String result = chatAiService.chat(req);
        return ResponseEntity.ok(new ChatResponseDto(result));
    }
}
