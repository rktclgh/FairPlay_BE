package com.fairing.fairplay.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDto {
    private List<ChatMessageDto> messages;
    private String providerOverride;     // "GEMINI" / "OPENAI" (선택)
    private Double temperature;          // 선택
    private Integer maxOutputTokens;     // 선택
}
