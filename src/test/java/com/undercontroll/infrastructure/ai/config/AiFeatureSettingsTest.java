package com.undercontroll.infrastructure.ai.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiFeatureSettingsTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @Test
    @DisplayName("parses provider aliases and rejects typos with the offending key")
    void parsesProvider() {
        assertThat(AiProvider.parse("ana", " Google-GenAI ")).isEqualTo(AiProvider.GEMINI);
        assertThat(AiProvider.parse("ana", "google_genai")).isEqualTo(AiProvider.GEMINI);
        assertThat(AiProvider.parse("ana", "OPENAI")).isEqualTo(AiProvider.OPENAI);
        assertThat(AiProvider.parse("ana", "")).isEqualTo(AiProvider.NONE);
        assertThat(AiProvider.parse("ana", null)).isEqualTo(AiProvider.NONE);
        assertThatThrownBy(() -> AiProvider.parse("insights", "gemni"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("undercontroll.ai.insights.provider='gemni'");
    }

    @Test
    @DisplayName("rejects unknown thinking values")
    void parsesThinking() {
        assertThat(ThinkingPolicy.parse("ana", " Low ")).isEqualTo(ThinkingPolicy.LOW);
        assertThat(ThinkingPolicy.parse("ana", null)).isEqualTo(ThinkingPolicy.DEFAULT);
        assertThatThrownBy(() -> ThinkingPolicy.parse("ana", "medium"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("undercontroll.ai.ana.thinking='medium'");
    }

    @Test
    @DisplayName("uses one default model per provider and keeps an explicit model")
    void resolvesModel() {
        assertThat(AiFeatureSettings.chat("ana", "gemini", " ", TIMEOUT, 0.2, 1024, "default").model())
                .isEqualTo("gemini-3.6-flash");
        assertThat(AiFeatureSettings.chat("ana", "openai", null, TIMEOUT, 0.2, 1024, "default").model())
                .isEqualTo("gpt-4.1-mini");
        assertThat(AiFeatureSettings.chat("ana", "gemini", "gemini-2.5-pro", TIMEOUT, 0.2, 1024, "default").model())
                .isEqualTo("gemini-2.5-pro");
        assertThat(AiFeatureSettings.transcription("transcription", "openai", "", TIMEOUT, "minimal").model())
                .isEqualTo("whisper-1");
        assertThat(AiFeatureSettings.transcription("transcription", "gemini", "", TIMEOUT, "minimal").model())
                .isEqualTo("gemini-3.5-flash-lite");
    }

    @Test
    @DisplayName("disabled feature has no model and ignores the rest")
    void disabled() {
        AiFeatureSettings settings = AiFeatureSettings.chat("ana", "none", "gpt-5", null, null, null, "high");

        assertThat(settings.enabled()).isFalse();
        assertThat(settings.model()).isNull();
        assertThat(settings.thinking()).isEqualTo(ThinkingPolicy.DEFAULT);
    }

    @Test
    @DisplayName("keeps supported thinking and drops it for models without thinking control")
    void thinkingByCapability() {
        assertThat(AiFeatureSettings.chat("insights", "gemini", "gemini-3.6-flash", TIMEOUT, 0.2, 4096, "low").thinking())
                .isEqualTo(ThinkingPolicy.LOW);
        assertThat(AiFeatureSettings.chat("insights", "gemini", "gemini-2.5-flash", TIMEOUT, 0.2, 4096, "off").thinking())
                .isEqualTo(ThinkingPolicy.OFF);
        assertThat(AiFeatureSettings.chat("insights", "openai", "gpt-5-mini", TIMEOUT, 0.2, 4096, "minimal").thinking())
                .isEqualTo(ThinkingPolicy.MINIMAL);
        assertThat(AiFeatureSettings.chat("insights", "openai", "gpt-4.1-mini", TIMEOUT, 0.2, 4096, "low").thinking())
                .isEqualTo(ThinkingPolicy.DEFAULT);
        assertThat(AiFeatureSettings.transcription("transcription", "openai", "whisper-1", TIMEOUT, "minimal").thinking())
                .isEqualTo(ThinkingPolicy.DEFAULT);
        assertThat(AiFeatureSettings.transcription("transcription", "gemini", "gemini-3.5-flash-lite", TIMEOUT, "minimal").thinking())
                .isEqualTo(ThinkingPolicy.MINIMAL);
    }

    @Test
    @DisplayName("fails fast on thinking the model cannot honor")
    void rejectsUnsupportedThinking() {
        assertThatThrownBy(() -> AiFeatureSettings.chat("insights", "gemini", "gemini-3.6-flash", TIMEOUT, 0.2, 4096, "off"))
                .hasMessageContaining("undercontroll.ai.insights.thinking=off");
        assertThatThrownBy(() -> AiFeatureSettings.chat("ana", "gemini", "gemini-3.6-flash", TIMEOUT, 0.2, 1024, "minimal"))
                .hasMessageContaining("undercontroll.ai.ana.thinking=minimal");
        assertThatThrownBy(() -> AiFeatureSettings.chat("ana", "gemini", "gemini-2.5-flash", TIMEOUT, 0.2, 1024, "minimal"))
                .hasMessageContaining("gemini-2.5-flash");
        assertThatThrownBy(() -> AiFeatureSettings.chat("ana", "gemini", "gemini-9-ultra", TIMEOUT, 0.2, 1024, "low"))
                .hasMessageContaining("gemini-9-ultra");
        assertThatThrownBy(() -> AiFeatureSettings.chat("ana", "openai", "o4-mini", TIMEOUT, 0.2, 1024, "off"))
                .hasMessageContaining("o4-mini");
    }

    @Test
    @DisplayName("validates timeout and output limit")
    void validatesLimits() {
        assertThatThrownBy(() -> AiFeatureSettings.chat("ana", "gemini", "", Duration.ZERO, 0.2, 1024, "default"))
                .hasMessageContaining("undercontroll.ai.ana.timeout");
        assertThatThrownBy(() -> AiFeatureSettings.chat("ana", "gemini", "", null, 0.2, 1024, "default"))
                .hasMessageContaining("undercontroll.ai.ana.timeout");
        assertThatThrownBy(() -> AiFeatureSettings.chat("ana", "gemini", "", TIMEOUT, 0.2, 0, "default"))
                .hasMessageContaining("undercontroll.ai.ana.max-output-tokens");
    }

    @Test
    @DisplayName("resolves capabilities from the configured model")
    void capabilities() {
        assertThat(ModelCapabilities.of(AiProvider.GEMINI, "gemini-3-transcribe").dedicatedTranscription()).isTrue();
        assertThat(ModelCapabilities.of(AiProvider.OPENAI, "o3").thinkingControl())
                .isEqualTo(ModelCapabilities.ThinkingControl.REASONING_EFFORT);
        assertThat(ModelCapabilities.of(AiProvider.NONE, null).thinkingControl())
                .isEqualTo(ModelCapabilities.ThinkingControl.NONE);
    }
}
