package com.fairing.fairplay.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fairing.fairplay.ai.config.LlmProperties;
import com.fairing.fairplay.ai.dto.ChatMessageDto;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HermesGatewayClient implements LlmClient {

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final Integer waitTimeoutSeconds;
    private final Integer connectTimeoutSeconds;
    private final Integer requestTimeoutSeconds;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HermesGatewayClient(LlmProperties props) {
        this.baseUrl = trimTrailingSlash(props.getHermesBaseUrl());
        this.apiKey = props.getHermesApiKey();
        this.model = props.getHermesModel();
        this.waitTimeoutSeconds = props.getHermesWaitTimeoutSeconds();
        this.connectTimeoutSeconds = props.getHermesConnectTimeoutSeconds();
        this.requestTimeoutSeconds = props.getHermesRequestTimeoutSeconds();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(1, connectTimeoutSeconds)))
                .build();
        if (this.baseUrl == null || this.baseUrl.isBlank()) {
            throw new IllegalStateException("Hermes gateway base URL is empty. Set llm.hermes.base-url.");
        }
    }

    @Override
    public String chat(List<ChatMessageDto> messages, Double temperature, Integer maxOutputTokens) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("prompt", toPrompt(messages));
        body.put("model", model);
        body.put("waitTimeoutSeconds", waitTimeoutSeconds);
        if (temperature != null) body.put("temperature", temperature);
        if (maxOutputTokens != null) body.put("maxOutputTokens", maxOutputTokens);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/generate"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("X-HRG-Service", "fairplay-be")
                .timeout(Duration.ofSeconds(Math.max(1, requestTimeoutSeconds)))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8));

        if (apiKey != null && !apiKey.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + apiKey);
        }

        HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new RuntimeException("Hermes gateway error: HTTP " + response.statusCode() + " - " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        if (response.statusCode() == 202) {
            String jobId = root.path("jobId").asText("");
            throw new RuntimeException("Hermes gateway timed out before completion. jobId=" + jobId);
        }
        return root.path("text").asText("");
    }

    private String toPrompt(List<ChatMessageDto> messages) {
        StringBuilder prompt = new StringBuilder();
        for (ChatMessageDto message : messages) {
            prompt.append(message.getRole().name()).append(": ")
                    .append(message.getContent() == null ? "" : message.getContent())
                    .append("\n\n");
        }
        prompt.append("ASSISTANT:");
        return prompt.toString();
    }

    private String trimTrailingSlash(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
