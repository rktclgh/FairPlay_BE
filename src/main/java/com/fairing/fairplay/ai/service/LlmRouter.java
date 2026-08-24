package com.fairing.fairplay.ai.service;

import com.fairing.fairplay.ai.client.BedrockLlmClient;
import com.fairing.fairplay.ai.client.GoogleGeminiClient;
import com.fairing.fairplay.ai.client.HermesGatewayClient;
import com.fairing.fairplay.ai.client.LlmClient;
import com.fairing.fairplay.ai.client.OpenAiClient;
import com.fairing.fairplay.ai.config.LlmProperties;
import org.springframework.stereotype.Component;

@Component
public class LlmRouter {

    private final LlmProperties props;
    private volatile GoogleGeminiClient gemini;
    private volatile OpenAiClient openai; // 지연 초기화
    private volatile HermesGatewayClient hermes; // 지연 초기화
    private volatile BedrockLlmClient bedrock; // 지연 초기화

    public LlmRouter(LlmProperties props) {
        this.props = props;
    }

    public synchronized LlmClient pick(String override) {
        String provider = (override != null && !override.isBlank()) ? override : props.getProvider();
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("LLM provider must be configured.");
        }
        return switch (provider.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "OPENAI" -> {
                if (openai == null) openai = new OpenAiClient(props);
                yield openai;
            }
            case "HERMES", "HERMES_REDIS_GATEWAY" -> {
                if (hermes == null) hermes = new HermesGatewayClient(props);
                yield hermes;
            }
            case "BEDROCK", "AWS_BEDROCK" -> {
                if (bedrock == null) bedrock = new BedrockLlmClient(props);
                yield bedrock;
            }
            case "GEMINI" -> {
                if (gemini == null) gemini = new GoogleGeminiClient(props);
                yield gemini;
            }
            default -> throw new IllegalArgumentException("Unsupported LLM provider: " + provider);
        };
    }
}
