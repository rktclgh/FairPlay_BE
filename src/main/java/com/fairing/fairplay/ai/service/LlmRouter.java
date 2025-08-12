package com.fairing.fairplay.ai.service;

import com.fairing.fairplay.ai.client.GoogleGeminiClient;
import com.fairing.fairplay.ai.client.LlmClient;
import com.fairing.fairplay.ai.client.OpenAiClient;
import com.fairing.fairplay.ai.config.LlmProperties;
import org.springframework.stereotype.Component;

@Component
public class LlmRouter {

    private final LlmProperties props;
    private final GoogleGeminiClient gemini;
    private OpenAiClient openai; // 지연 초기화

    public LlmRouter(LlmProperties props) {
        this.props = props;
        this.gemini = new GoogleGeminiClient(props);
        if (props.getOpenaiApiKey() != null && !props.getOpenaiApiKey().isBlank()) {
            try { this.openai = new OpenAiClient(props); } catch (Exception ignored) {}
        }
    }

    public LlmClient pick(String override) {
        String provider = (override != null && !override.isBlank()) ? override : props.getProvider();
        if ("OPENAI".equalsIgnoreCase(provider)) {
            if (openai == null) openai = new OpenAiClient(props);
            return openai;
        }
        return gemini;
    }
}
