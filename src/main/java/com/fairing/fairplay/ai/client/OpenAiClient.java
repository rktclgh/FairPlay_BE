package com.fairing.fairplay.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fairing.fairplay.ai.config.LlmProperties;
import com.fairing.fairplay.ai.dto.ChatMessageDto;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class OpenAiClient implements LlmClient {

    private final String apiKey;
    private final String model;
    private final ObjectMapper om = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    public OpenAiClient(LlmProperties props) {
        this.apiKey = props.getOpenaiApiKey();
        this.model = props.getOpenaiModel();
        if (this.apiKey == null || this.apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI API KEY가 비어 있습니다. 환경변수 OPENAI_API_KEY 또는 llm.openai.api-key 설정을 확인하세요.");
        }
    }

    @Override
    public String chat(List<ChatMessageDto> messages, Double temperature, Integer maxTokens) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        if (temperature != null) body.put("temperature", temperature);
        if (maxTokens != null) body.put("max_tokens", maxTokens);

        List<Map<String, String>> msgs = new ArrayList<>();
        for (ChatMessageDto m : messages) {
            String role = switch (m.getRole()) {
                case SYSTEM -> "system";
                case USER -> "user";
                case ASSISTANT -> "assistant";
            };
            msgs.add(Map.of("role", role, "content", m.getContent()));
        }
        body.put("messages", msgs);

        String json = om.writeValueAsString(body);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() / 100 != 2) {
            throw new RuntimeException("OpenAI API 오류: HTTP " + res.statusCode() + " - " + res.body());
        }

        JsonNode root = om.readTree(res.body());
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) return "";
        JsonNode content = choices.get(0).path("message").path("content");
        return content.isMissingNode() ? "" : content.asText();
    }
}
