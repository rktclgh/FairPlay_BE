package com.fairing.fairplay.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fairing.fairplay.ai.dto.ChatMessageDto;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BedrockLlmClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sendsNovaMessageSchemaWithoutLoggingOrFlatteningTheConversation() throws Exception {
        BedrockRuntimeClient bedrock = mock(BedrockRuntimeClient.class);
        when(bedrock.invokeModel(any(InvokeModelRequest.class))).thenReturn(responseWithText("Bedrock 응답"));
        BedrockLlmClient client = new BedrockLlmClient(
                "apac.amazon.nova-micro-v1:0", 0.2, 0.9, 2048, bedrock, objectMapper
        );

        String response = client.chat(List.of(
                ChatMessageDto.system("시스템 지침"),
                ChatMessageDto.user("사용자 질문"),
                ChatMessageDto.assistant("이전 답변")
        ), null, null);

        assertThat(response).isEqualTo("Bedrock 응답");
        org.mockito.ArgumentCaptor<InvokeModelRequest> requestCaptor =
                org.mockito.ArgumentCaptor.forClass(InvokeModelRequest.class);
        org.mockito.Mockito.verify(bedrock).invokeModel(requestCaptor.capture());

        InvokeModelRequest request = requestCaptor.getValue();
        assertThat(request.modelId()).isEqualTo("apac.amazon.nova-micro-v1:0");
        JsonNode body = objectMapper.readTree(request.body().asUtf8String());
        assertThat(body.path("schemaVersion").asText()).isEqualTo("messages-v1");
        assertThat(body.path("system").get(0).path("text").asText()).isEqualTo("시스템 지침");
        assertThat(body.path("messages").get(0).path("role").asText()).isEqualTo("user");
        assertThat(body.path("messages").get(0).path("content").get(0).path("text").asText())
                .isEqualTo("사용자 질문");
        assertThat(body.path("messages").get(1).path("role").asText()).isEqualTo("assistant");
        assertThat(body.path("inferenceConfig").path("maxTokens").asInt()).isEqualTo(2048);
        assertThat(body.path("inferenceConfig").path("temperature").asDouble()).isEqualTo(0.2);
        assertThat(body.path("inferenceConfig").path("topP").asDouble()).isEqualTo(0.9);
    }

    @Test
    void usesPerRequestGenerationValuesWhenProvided() throws Exception {
        BedrockRuntimeClient bedrock = mock(BedrockRuntimeClient.class);
        when(bedrock.invokeModel(any(InvokeModelRequest.class))).thenReturn(responseWithText("응답"));
        BedrockLlmClient client = new BedrockLlmClient(
                "apac.amazon.nova-micro-v1:0", 0.2, 0.9, 2048, bedrock, objectMapper
        );

        client.chat(List.of(ChatMessageDto.user("질문")), 0.6, 512);

        org.mockito.ArgumentCaptor<InvokeModelRequest> requestCaptor =
                org.mockito.ArgumentCaptor.forClass(InvokeModelRequest.class);
        org.mockito.Mockito.verify(bedrock).invokeModel(requestCaptor.capture());
        JsonNode body = objectMapper.readTree(requestCaptor.getValue().body().asUtf8String());
        assertThat(body.path("inferenceConfig").path("maxTokens").asInt()).isEqualTo(512);
        assertThat(body.path("inferenceConfig").path("temperature").asDouble()).isEqualTo(0.6);
    }

    @Test
    void normalizesAssistantFirstHistoryToAValidNovaConversation() throws Exception {
        BedrockRuntimeClient bedrock = mock(BedrockRuntimeClient.class);
        when(bedrock.invokeModel(any(InvokeModelRequest.class))).thenReturn(responseWithText("응답"));
        BedrockLlmClient client = new BedrockLlmClient(
                "apac.amazon.nova-micro-v1:0", 0.2, 0.9, 2048, bedrock, objectMapper
        );

        client.chat(List.of(ChatMessageDto.assistant("이전 답변"), ChatMessageDto.user("새 질문")), null, null);

        org.mockito.ArgumentCaptor<InvokeModelRequest> requestCaptor =
                org.mockito.ArgumentCaptor.forClass(InvokeModelRequest.class);
        org.mockito.Mockito.verify(bedrock).invokeModel(requestCaptor.capture());
        JsonNode messages = objectMapper.readTree(requestCaptor.getValue().body().asUtf8String()).path("messages");
        assertThat(messages.get(0).path("role").asText()).isEqualTo("user");
        assertThat(messages.get(1).path("role").asText()).isEqualTo("assistant");
        assertThat(messages.get(2).path("role").asText()).isEqualTo("user");
    }

    @Test
    void rejectsMalformedResponsesWithoutIncludingTheirBodyInTheError() throws Exception {
        BedrockRuntimeClient bedrock = mock(BedrockRuntimeClient.class);
        when(bedrock.invokeModel(any(InvokeModelRequest.class))).thenReturn(
                InvokeModelResponse.builder().body(SdkBytes.fromUtf8String("{\"sensitive\":\"must-not-escape\"}")).build()
        );
        BedrockLlmClient client = new BedrockLlmClient(
                "apac.amazon.nova-micro-v1:0", 0.2, 0.9, 2048, bedrock, objectMapper
        );

        assertThatThrownBy(() -> client.chat(List.of(ChatMessageDto.user("질문")), null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid response body")
                .hasMessageNotContaining("must-not-escape");
    }

    private InvokeModelResponse responseWithText(String text) {
        String responseBody = "{\"output\":{\"message\":{\"content\":[{\"text\":\"" + text + "\"}]}}}";
        return InvokeModelResponse.builder().body(SdkBytes.fromUtf8String(responseBody)).build();
    }
}
