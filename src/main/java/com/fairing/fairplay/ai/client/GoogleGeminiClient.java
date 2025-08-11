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
        boolean beta = useBetaEndpoint(model);
        boolean allowThinking = beta && supportsThinking(model);

        int outTokens = (maxOutputTokens != null && maxOutputTokens > 0) ? maxOutputTokens : 512; // 필요하면 1024로 올려도 됨
        double temp = (temperature != null) ? temperature : 0.7;

        // 1차 시도
        CallResult r = sendOnce(messages, temp, outTokens, beta, /*thinkingBudget*/ allowThinking ? 64 : null);
        if (r.ok()) return r.text();

        // 2차 시도 (토큰 컷 or thoughts에 토큰 먹힌 케이스 대비)
        if (r.shouldRetry) {
            int out2 = Math.min(outTokens * 2, 4096);
            CallResult r2 = sendOnce(messages, temp, out2, beta, /*thinkingBudget*/ allowThinking ? 0 : null);
            if (r2.ok()) return r2.text();
        }

        // 마지막 방어 메시지
        return (r.finishReason != null && r.finishReason.equalsIgnoreCase("MAX_TOKENS"))
                ? "응답이 길어서 잘렸어요. 질문을 더 짧게 하거나 maxOutputTokens를 늘려주세요."
                : "";
    }

    private CallResult sendOnce(List<ChatMessageDto> messages,
                                double temperature,
                                int maxOutputTokens,
                                boolean useV1beta,
                                Integer thinkingBudget) throws Exception {

        String url = "https://generativelanguage.googleapis.com/"
                + (useV1beta ? "v1beta" : "v1")
                + "/models/" + model + ":generateContent";

        Map<String, Object> body = buildBody(messages, temperature, maxOutputTokens, useV1beta, thinkingBudget);
        String json = om.writeValueAsString(body);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());

        // 400 & thinking 미지원 → thinking 제거하고 1회 재시도
        if (res.statusCode() == 400 && res.body() != null && res.body().contains("Unknown name \"thinking\"")) {
            log.warn("Model does not support 'thinking'. Retrying without thinking...");
            body = buildBody(messages, temperature, maxOutputTokens, useV1beta, null);
            json = om.writeValueAsString(body);
            req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            res = http.send(req, HttpResponse.BodyHandlers.ofString());
        }

        if (res.statusCode() / 100 != 2) {
            log.error("Gemini API 오류: HTTP {} - {}", res.statusCode(), res.body());
            throw new RuntimeException("Gemini API 오류: HTTP " + res.statusCode());
        }

        if (log.isDebugEnabled()) {
            log.debug("Gemini raw response: {}", res.body());
        }

        JsonNode root = om.readTree(res.body());
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            JsonNode pf = root.path("promptFeedback");
            if (!pf.isMissingNode()) log.warn("[Gemini] 빈 응답. promptFeedback={}", pf.toString());
            return CallResult.empty("UNKNOWN", /*retry*/true);
        }

        JsonNode first = candidates.get(0);
        String finish = first.path("finishReason").asText("");

        // 텍스트 추출
        StringBuilder out = new StringBuilder();
        JsonNode parts = first.path("content").path("parts");
        if (parts.isArray()) {
            for (JsonNode p : parts) {
                JsonNode t = p.path("text");
                if (!t.isMissingNode()) out.append(t.asText());
            }
        }
        if (out.length() == 0) {
            JsonNode alt = first.path("content").path("text");
            if (!alt.isMissingNode()) out.append(alt.asText());
        }

        if ("SAFETY".equalsIgnoreCase(finish)) {
            log.warn("Gemini SAFETY 종료: {}", res.body());
            return CallResult.empty(finish, false);
        }
        if ("MAX_TOKENS".equalsIgnoreCase(finish) && out.length() == 0) {
            // thoughts가 토큰을 다 써서 텍스트가 비었을 가능성 → 재시도 권장
            log.warn("Gemini MAX_TOKENS 종료 (텍스트 없음). outMax={}, useV1beta={}, thinking={}", maxOutputTokens, useV1beta, thinkingBudget);
            return CallResult.empty(finish, true);
        }

        return CallResult.ok(out.toString().trim(), finish);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildBody(List<ChatMessageDto> messages,
                                          double temperature,
                                          int maxOutputTokens,
                                          boolean useV1beta,
                                          Integer thinkingBudget) {

        StringBuilder sys = new StringBuilder();
        List<Map<String, Object>> contents = new ArrayList<>();

        for (ChatMessageDto m : messages) {
            switch (m.getRole()) {
                case SYSTEM -> sys.append(m.getContent()).append("\n");
                case USER -> contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", m.getContent()))));
                case ASSISTANT -> contents.add(Map.of("role", "model", "parts", List.of(Map.of("text", m.getContent()))));
            }
        }

        Map<String, Object> body = new HashMap<>();

        if (useV1beta) {
            // v1beta: systemInstruction 사용 가능
            if (sys.length() > 0) {
                body.put("systemInstruction",
                        Map.of("role", "user", "parts", List.of(Map.of("text", sys.toString().trim()))));
            }
        } else {
            // v1: SYSTEM을 첫 user 프롬프트에 prepend
            if (sys.length() > 0) {
                String s = sys.toString().trim();
                if (!contents.isEmpty() && "user".equals(contents.get(0).get("role"))) {
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) contents.get(0).get("parts");
                    String orig = (String) parts.get(0).get("text");
                    parts.set(0, Map.of("text", s + "\n\n" + orig));
                } else {
                    contents.add(0, Map.of("role", "user", "parts", List.of(Map.of("text", s))));
                }
            }
        }

        body.put("contents", contents);

        Map<String, Object> gen = new HashMap<>();
        gen.put("temperature", temperature);
        gen.put("maxOutputTokens", maxOutputTokens);
        if (useV1beta) {
            gen.put("responseMimeType", "text/plain"); // 깔끔한 텍스트만
        }
        body.put("generationConfig", gen);

        // thinking: v1beta + 지원 모델 + budget 있을 때만
        if (useV1beta && thinkingBudget != null && supportsThinking(model)) {
            body.put("thinking", Map.of("budgetTokens", thinkingBudget));
        }

        return body;
    }

    // --- 모델/엔드포인트 헬퍼 ---

    private static boolean supportsThinking(String model) {
        // -thinking/-reasoning 등 사고 체인 노출 모델만 true
        return model != null && (model.contains("-thinking") || model.contains("-reasoning"));
    }

    private static boolean useBetaEndpoint(String model) {
        // 2.0/2.5 계열은 보통 v1beta 엔드포인트 사용이 안전
        return model != null && (model.startsWith("gemini-2.5") || model.startsWith("gemini-2.0"));
    }

    // --- 결과 래퍼 ---

    private record CallResult(String text, String finishReason, boolean shouldRetry) {
        static CallResult ok(String t, String finish) { return new CallResult(t, finish, false); }
        static CallResult empty(String finish, boolean retry) { return new CallResult("", finish, retry); }
        boolean ok() { return text != null && !text.isBlank(); }
        public String text() { return text; } // accessor는 public이어야 컴파일 오류 안 남
    }
}
