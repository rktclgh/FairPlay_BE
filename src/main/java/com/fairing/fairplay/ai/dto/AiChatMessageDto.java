package com.fairing.fairplay.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatMessageDto {
    private String type; // "user_message", "ai_response", "system_error"
    private String content;
    private Long chatRoomId;
    private Long senderId;
    private String timestamp;
    private String provider; // "gemini", "openai"
    private Double temperature;
    private Integer maxOutputTokens;
}