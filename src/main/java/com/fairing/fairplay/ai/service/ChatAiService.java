package com.fairing.fairplay.ai.service;

import com.fairing.fairplay.ai.client.LlmClient;
import com.fairing.fairplay.ai.dto.ChatMessageDto;
import com.fairing.fairplay.ai.dto.ChatRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatAiService {

    private final LlmRouter llmRouter;

    public String chat(ChatRequestDto req) throws Exception {
        List<ChatMessageDto> messages = req.getMessages();
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("messages가 비어 있습니다.");
        }
        LlmClient client = llmRouter.pick(req.getProviderOverride());
        return client.chat(messages, req.getTemperature(), req.getMaxOutputTokens());
    }
}
