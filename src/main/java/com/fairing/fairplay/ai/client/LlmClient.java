package com.fairing.fairplay.ai.client;

import com.fairing.fairplay.ai.dto.ChatMessageDto;

import java.util.List;

public interface LlmClient {
    String chat(List<ChatMessageDto> messages, Double temperature, Integer maxOutputTokens) throws Exception;
}
