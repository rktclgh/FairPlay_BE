package com.fairing.fairplay.ai.service;

import com.fairing.fairplay.ai.client.BedrockLlmClient;
import com.fairing.fairplay.ai.client.HermesGatewayClient;
import com.fairing.fairplay.ai.config.LlmProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmRouterTest {

    @Test
    void selectsBedrockWithoutRequiringTheChatGeminiApiKey() {
        LlmProperties properties = mock(LlmProperties.class);
        when(properties.getProvider()).thenReturn("BEDROCK");
        when(properties.getBedrockEnabled()).thenReturn(true);
        when(properties.getBedrockRegion()).thenReturn("ap-northeast-2");
        when(properties.getBedrockModelId()).thenReturn("apac.amazon.nova-micro-v1:0");
        when(properties.getBedrockTemperature()).thenReturn(0.2);
        when(properties.getBedrockTopP()).thenReturn(0.9);
        when(properties.getBedrockMaxOutputTokens()).thenReturn(2048);
        when(properties.getBedrockRequestTimeoutSeconds()).thenReturn(60);

        assertThat(new LlmRouter(properties).pick(null)).isInstanceOf(BedrockLlmClient.class);
    }

    @Test
    void keepsHermesAvailableOnlyWhenExplicitlySelected() {
        LlmProperties properties = mock(LlmProperties.class);
        when(properties.getHermesBaseUrl()).thenReturn("http://127.0.0.1:8788");
        when(properties.getHermesApiKey()).thenReturn("");
        when(properties.getHermesModel()).thenReturn("hermes");
        when(properties.getHermesWaitTimeoutSeconds()).thenReturn(60);
        when(properties.getHermesConnectTimeoutSeconds()).thenReturn(5);
        when(properties.getHermesRequestTimeoutSeconds()).thenReturn(75);

        assertThat(new LlmRouter(properties).pick("HERMES")).isInstanceOf(HermesGatewayClient.class);
    }

    @Test
    void rejectsUnknownProvidersInsteadOfFallingBackToGemini() {
        LlmProperties properties = mock(LlmProperties.class);
        when(properties.getProvider()).thenReturn("BEDROK");

        assertThatThrownBy(() -> new LlmRouter(properties).pick(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported LLM provider: BEDROK");
    }
}
