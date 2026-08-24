package com.fairing.fairplay.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fairing.fairplay.ai.config.LlmProperties;
import com.fairing.fairplay.ai.dto.ChatMessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.BedrockRuntimeException;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.time.Duration;
import java.util.List;

/** Amazon Bedrock Amazon Nova messages-v1 호출 전용 클라이언트. */
public class BedrockLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(BedrockLlmClient.class);

    private final String modelId;
    private final double defaultTemperature;
    private final double defaultTopP;
    private final int defaultMaxOutputTokens;
    private final BedrockRuntimeClient bedrock;
    private final ObjectMapper objectMapper;

    public BedrockLlmClient(LlmProperties props) {
        if (!Boolean.TRUE.equals(props.getBedrockEnabled())) {
            throw new IllegalStateException("Bedrock is disabled. Set BEDROCK_ENABLED=true before selecting LLM_PROVIDER=BEDROCK.");
        }

        this.modelId = requireText(props.getBedrockModelId(), "BEDROCK_MODEL_ID");
        String region = requireText(props.getBedrockRegion(), "BEDROCK_REGION");
        this.defaultTemperature = boundedOrDefault(props.getBedrockTemperature(), 0.2, "BEDROCK_TEMPERATURE");
        this.defaultTopP = boundedOrDefault(props.getBedrockTopP(), 0.9, "BEDROCK_TOP_P");
        this.defaultMaxOutputTokens = outputTokensOrDefault(props.getBedrockMaxOutputTokens(), 2048);
        int requestTimeoutSeconds = positiveOrDefault(props.getBedrockRequestTimeoutSeconds(), 60);
        this.objectMapper = new ObjectMapper();
        this.bedrock = BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(Duration.ofSeconds(requestTimeoutSeconds))
                        .build())
                .build();
    }

    BedrockLlmClient(
            String modelId,
            double defaultTemperature,
            double defaultTopP,
            int defaultMaxOutputTokens,
            BedrockRuntimeClient bedrock,
            ObjectMapper objectMapper
    ) {
        this.modelId = modelId;
        this.defaultTemperature = defaultTemperature;
        this.defaultTopP = defaultTopP;
        this.defaultMaxOutputTokens = defaultMaxOutputTokens;
        this.bedrock = bedrock;
        this.objectMapper = objectMapper;
    }

    @Override
    public String chat(List<ChatMessageDto> messages, Double temperature, Integer maxOutputTokens) throws Exception {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("Bedrock chat requires at least one message.");
        }

        int effectiveMaxTokens = maxOutputTokens != null
                ? outputTokensOrDefault(maxOutputTokens, defaultMaxOutputTokens) : defaultMaxOutputTokens;
        double effectiveTemperature = temperature != null
                ? boundedOrDefault(temperature, defaultTemperature, "temperature") : defaultTemperature;
        ObjectNode body = buildRequestBody(messages, effectiveTemperature, effectiveMaxTokens);

        log.info("Bedrock LLM request: modelId={}, messageCount={}, maxOutputTokens={}",
                modelId, messages.size(), effectiveMaxTokens);
        try {
            InvokeModelResponse response = bedrock.invokeModel(InvokeModelRequest.builder()
                    .modelId(modelId)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromUtf8String(objectMapper.writeValueAsString(body)))
                    .build());
            return extractText(response);
        } catch (BedrockRuntimeException exception) {
            log.warn("Bedrock LLM request failed: modelId={}, statusCode={}, errorCode={}",
                    modelId, exception.statusCode(), exception.awsErrorDetails() == null
                            ? "unknown" : exception.awsErrorDetails().errorCode());
            throw new RuntimeException("Bedrock LLM request failed: HTTP " + exception.statusCode(), exception);
        }
    }

    private ObjectNode buildRequestBody(List<ChatMessageDto> messages, double temperature, int maxOutputTokens) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("schemaVersion", "messages-v1");

        ArrayNode system = body.putArray("system");
        ArrayNode conversation = body.putArray("messages");
        boolean firstConversationTurn = true;
        ChatMessageDto.Role previousRole = null;
        for (ChatMessageDto message : messages) {
            if (message == null || message.getRole() == null) {
                throw new IllegalArgumentException("Bedrock chat messages must have a role.");
            }
            String content = message.getContent() == null ? "" : message.getContent();
            if (message.getRole() == ChatMessageDto.Role.SYSTEM) {
                system.addObject().put("text", content);
                continue;
            }

            if (firstConversationTurn && message.getRole() == ChatMessageDto.Role.ASSISTANT) {
                conversation.addObject()
                        .put("role", "user")
                        .putArray("content")
                        .addObject().put("text", "Continue the existing conversation from its prior context.");
                previousRole = ChatMessageDto.Role.USER;
                firstConversationTurn = false;
            }
            if (previousRole == message.getRole()) {
                throw new IllegalArgumentException("Bedrock chat messages must alternate user and assistant roles.");
            }

            ObjectNode item = conversation.addObject();
            item.put("role", message.getRole() == ChatMessageDto.Role.ASSISTANT ? "assistant" : "user");
            item.putArray("content").addObject().put("text", content);
            previousRole = message.getRole();
            firstConversationTurn = false;
        }
        if (conversation.isEmpty()) {
            throw new IllegalArgumentException("Bedrock chat requires at least one user or assistant message.");
        }

        ObjectNode inferenceConfig = body.putObject("inferenceConfig");
        inferenceConfig.put("maxTokens", maxOutputTokens);
        inferenceConfig.put("temperature", temperature);
        inferenceConfig.put("topP", defaultTopP);
        return body;
    }

    private String extractText(InvokeModelResponse response) throws Exception {
        JsonNode content = objectMapper.readTree(response.body().asUtf8String())
                .path("output").path("message").path("content");
        if (!content.isArray()) {
            throw new IllegalStateException("Bedrock LLM returned an invalid response body for modelId=" + modelId);
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode item : content) {
            if (item.hasNonNull("text")) {
                text.append(item.path("text").asText());
            }
        }
        String result = text.toString().trim();
        if (result.isBlank()) {
            throw new IllegalStateException("Bedrock LLM returned no text for modelId=" + modelId);
        }
        return result;
    }

    private static String requireText(String value, String envName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(envName + " must be configured for Bedrock.");
        }
        return value.trim();
    }

    private static int positiveOrDefault(Integer value, int fallback) {
        return value != null && value > 0 ? value : fallback;
    }

    private static double boundedOrDefault(Double value, double fallback, String settingName) {
        if (value == null) {
            return fallback;
        }
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException(settingName + " must be between 0 and 1.");
        }
        return value;
    }

    private static int outputTokensOrDefault(Integer value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value < 1 || value > 10_000) {
            throw new IllegalArgumentException("maxOutputTokens must be between 1 and 10000.");
        }
        return value;
    }
}
