package com.fairing.fairplay.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChatRequestDto {
    private List<ChatMessageDto> messages;
    private String providerOverride;     // "GEMINI" / "OPENAI" (선택)
    private Double temperature;          // 선택
    private Integer maxOutputTokens;     // 선택
}
