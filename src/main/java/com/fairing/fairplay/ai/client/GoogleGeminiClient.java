package com.fairing.fairplay.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fairing.fairplay.ai.config.LlmProperties;
import com.fairing.fairplay.ai.dto.ChatMessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GoogleGeminiClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleGeminiClient.class);

    private final String apiKey;
    private final String model;
    private final ObjectMapper om = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    public GoogleGeminiClient(LlmProperties props) {
        this.apiKey = props.getGeminiApiKey();
        this.model = props.getGeminiModel();
        if (this.apiKey == null || this.apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI API KEY가 비어 있습니다. 환경변수 GEMINI_API_KEY 또는 llm.gemini.api-key 설정을 확인하세요.");
        }
    }

    @Override
    public String chat(List<ChatMessageDto> messages, Double temperature, Integer maxOutputTokens) throws Exception {
        Map<String, Object> body = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();

        // SYSTEM을 user 선두 프롬프트에 합치기 (필요시 systemInstruction로 분리 가능)
        for (ChatMessageDto m : messages) {
            String role = switch (m.getRole()) {
                case SYSTEM -> "user";
                case USER -> "user";
                case ASSISTANT -> "model";
            };
            Map<String, Object> part = Map.of("text", m.getContent());
            Map<String, Object> content = new HashMap<>();
            content.put("role", role);
            content.put("parts", List.of(part));
            contents.add(content);
        }
        body.put("contents", contents);

        Map<String, Object> generationConfig = new HashMap<>();
        if (temperature != null) generationConfig.put("temperature", temperature);
        if (maxOutputTokens != null) generationConfig.put("maxOutputTokens", maxOutputTokens);
        if (!generationConfig.isEmpty()) body.put("generationConfig", generationConfig);

        String url = "https://generativelanguage.googleapis.com/v1/models/" + model + ":generateContent";
        String json = om.writeValueAsString(body);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());

        // 1) 상태코드 체크
        if (res.statusCode() / 100 != 2) {
            log.error("Gemini API 오류: HTTP {} - {}", res.statusCode(), res.body());
            throw new RuntimeException("Gemini API 오류: HTTP " + res.statusCode());
        }

        // 2) 원시 응답 로깅(초기 디버그용) — 필요 없으면 주석 처리
        if (log.isDebugEnabled()) {
            log.debug("Gemini raw response: {}", res.body());
        }

        // 3) 파싱
        JsonNode root = om.readTree(res.body());

        // SAFETY 차단 여부/사유 확인
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            // promptFeedback에 차단 사유가 있을 수 있음
            JsonNode pf = root.path("promptFeedback");
            if (!pf.isMissingNode()) {
                StringBuilder sb = new StringBuilder();
                sb.append("[Gemini] 응답이 차단되었을 수 있습니다.");
                JsonNode blockReason = pf.path("blockReason");
                if (!blockReason.isMissingNode()) sb.append(" blockReason=").append(blockReason.asText());
                JsonNode safetyRatings = pf.path("safetyRatings");
                if (safetyRatings.isArray()) sb.append(" safetyRatings=").append(safetyRatings.toString());
                log.warn(sb.toString());
                return ""; // 프론트엔드가 빈 문자열을 감지해서 안내하도록 유지
            }
            return "";
        }

        JsonNode first = candidates.get(0);
        String finish = first.path("finishReason").asText(""); // STOP | SAFETY | ...
        if ("SAFETY".equalsIgnoreCase(finish)) {
            log.warn("Gemini 응답이 SAFETY로 종료되어 내용이 비어 있을 수 있습니다. resp={}", res.body());
            return "";
        }

        // 정상 텍스트 추출: parts[*].text 모두 이어붙이기
        JsonNode parts = first.path("content").path("parts");
        if (parts.isArray() && parts.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode p : parts) {
                JsonNode t = p.path("text");
                if (!t.isMissingNode()) {
                    sb.append(t.asText());
                }
            }
            return sb.toString();
        }

        // 혹시 다른 구조일 경우를 대비한 안전장치
        JsonNode altText = first.path("content").path("text");
        if (!altText.isMissingNode()) {
            return altText.asText();
        }

        // 여기까지 못 찾으면 빈 문자열
        log.warn("Gemini 응답에서 텍스트를 찾지 못했습니다. resp={}", res.body());
        return "";
    }
}
