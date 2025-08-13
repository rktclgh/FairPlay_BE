package com.fairing.fairplay.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LlmProperties {

    @Value("${llm.provider:GEMINI}")
    private String provider;

    @Value("${llm.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${llm.gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    @Value("${llm.openai.api-key:}")
    private String openaiApiKey;

    @Value("${llm.openai.model:gpt-4o-mini}")
    private String openaiModel;

    public String getProvider() { return provider; }
    public String getGeminiApiKey() { return geminiApiKey; }
    public String getGeminiModel() { return geminiModel; }
    public String getOpenaiApiKey() { return openaiApiKey; }
    public String getOpenaiModel() { return openaiModel; }
}
