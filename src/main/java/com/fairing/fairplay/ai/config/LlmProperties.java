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

    @Value("${llm.hermes.base-url:http://127.0.0.1:8788}")
    private String hermesBaseUrl;

    @Value("${llm.hermes.api-key:}")
    private String hermesApiKey;

    @Value("${llm.hermes.model:gpt-5.4-mini}")
    private String hermesModel;

    @Value("${llm.hermes.wait-timeout-seconds:60}")
    private Integer hermesWaitTimeoutSeconds;

    @Value("${llm.hermes.connect-timeout-seconds:5}")
    private Integer hermesConnectTimeoutSeconds;

    @Value("${llm.hermes.request-timeout-seconds:75}")
    private Integer hermesRequestTimeoutSeconds;

    public String getProvider() { return provider; }
    public String getGeminiApiKey() { return geminiApiKey; }
    public String getGeminiModel() { return geminiModel; }
    public String getOpenaiApiKey() { return openaiApiKey; }
    public String getOpenaiModel() { return openaiModel; }
    public String getHermesBaseUrl() { return hermesBaseUrl; }
    public String getHermesApiKey() { return hermesApiKey; }
    public String getHermesModel() { return hermesModel; }
    public Integer getHermesWaitTimeoutSeconds() { return hermesWaitTimeoutSeconds; }
    public Integer getHermesConnectTimeoutSeconds() { return hermesConnectTimeoutSeconds; }
    public Integer getHermesRequestTimeoutSeconds() { return hermesRequestTimeoutSeconds; }
}
