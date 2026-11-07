package com.undercontroll.infrastructure.ai.config;

import com.google.genai.types.HttpOptions;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.google.genai.common.GoogleGenAiThinkingLevel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatModelFactoryTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(45);

    @Test
    @DisplayName("builds a Gemini model with the requested model, limits and thinking")
    void buildsGemini() {
        AiFeatureSettings settings = AiFeatureSettings.chat("insights", "gemini", "gemini-3.6-flash", TIMEOUT, 0.2, 4096, "low");

        ChatModel model = factory("key", "").create(settings);

        GoogleGenAiChatOptions options = (GoogleGenAiChatOptions) assertThat(model)
                .isInstanceOf(GoogleGenAiChatModel.class)
                .actual()
                .getDefaultOptions();
        assertThat(options.getModel()).isEqualTo("gemini-3.6-flash");
        assertThat(options.getMaxOutputTokens()).isEqualTo(4096);
        assertThat(options.getThinkingLevel()).isEqualTo(GoogleGenAiThinkingLevel.LOW);
        assertThat(options.getThinkingBudget()).isNull();
    }

    @Test
    @DisplayName("maps thinking to a budget on Gemini 2.5 and omits it by default")
    void geminiThinking() {
        GoogleGenAiChatOptions off = ChatModelFactory.geminiOptions(
                AiFeatureSettings.chat("ana", "gemini", "gemini-2.5-flash", TIMEOUT, 0.2, 1024, "off"));
        GoogleGenAiChatOptions high = ChatModelFactory.geminiOptions(
                AiFeatureSettings.chat("ana", "gemini", "gemini-3.6-flash", TIMEOUT, 0.2, 1024, "high"));
        GoogleGenAiChatOptions none = ChatModelFactory.geminiOptions(
                AiFeatureSettings.chat("ana", "gemini", "gemini-3.6-flash", TIMEOUT, 0.2, 1024, "default"));

        assertThat(off.getThinkingBudget()).isZero();
        assertThat(high.getThinkingLevel()).isEqualTo(GoogleGenAiThinkingLevel.HIGH);
        assertThat(none.getThinkingLevel()).isNull();
        assertThat(none.getThinkingBudget()).isNull();
        assertThat(ThinkingPolicy.HIGH.budgetTokens()).isEqualTo(8192);
        assertThat(ThinkingPolicy.LOW.budgetTokens()).isEqualTo(1024);
    }

    @Test
    @DisplayName("builds an OpenAI model and uses reasoning effort only on reasoning models")
    void buildsOpenAi() {
        ChatModel model = factory("", "sk-test")
                .create(AiFeatureSettings.chat("ana", "openai", "", TIMEOUT, 0.2, 1024, "low"));
        OpenAiChatOptions reasoning = ChatModelFactory.openAiOptions(
                AiFeatureSettings.chat("ana", "openai", "gpt-5-mini", TIMEOUT, 0.2, 1024, "low"));
        OpenAiChatOptions reasoningDefault = ChatModelFactory.openAiOptions(
                AiFeatureSettings.chat("ana", "openai", "gpt-5-mini", TIMEOUT, 0.2, 1024, "default"));

        OpenAiChatOptions options = (OpenAiChatOptions) assertThat(model)
                .isInstanceOf(OpenAiChatModel.class)
                .actual()
                .getDefaultOptions();
        assertThat(options.getModel()).isEqualTo("gpt-4.1-mini");
        assertThat(options.getMaxTokens()).isEqualTo(1024);
        assertThat(options.getReasoningEffort()).isNull();
        assertThat(reasoning.getReasoningEffort()).isEqualTo("low");
        assertThat(reasoning.getMaxCompletionTokens()).isEqualTo(1024);
        assertThat(reasoning.getMaxTokens()).isNull();
        assertThat(reasoningDefault.getReasoningEffort()).isNull();
    }

    @Test
    @DisplayName("fails with the missing credential named")
    void requiresCredentials() {
        ChatModelFactory factory = factory("", "");

        assertThatThrownBy(() -> factory.create(AiFeatureSettings.chat("ana", "gemini", "", TIMEOUT, 0.2, 1024, "default")))
                .hasMessageContaining("undercontroll.ai.ana.provider=gemini requires GEMINI_API_KEY");
        assertThatThrownBy(() -> factory.create(AiFeatureSettings.chat("insights", "openai", "", TIMEOUT, 0.2, 1024, "default")))
                .hasMessageContaining("undercontroll.ai.insights.provider=openai requires OPENAI_API_KEY");
        assertThatThrownBy(() -> factory.openAiAudioApi(AiFeatureSettings.transcription("transcription", "openai", "", TIMEOUT, "default")))
                .hasMessageContaining("OPENAI_API_KEY");
        assertThatThrownBy(() -> factory.create(AiFeatureSettings.chat("ana", "none", "", TIMEOUT, 0.2, 1024, "default")))
                .hasMessageContaining("disabled");
    }

    @Test
    @DisplayName("uses Vertex AI when project and location are set without an API key")
    void vertexClient() {
        ChatModelFactory factory = new ChatModelFactory(
                new StaticListableBeanFactory().getBeanProvider(ObservationRegistry.class),
                "", "project", "us-central1", "", "https://api.openai.com");

        assertThat(factory.geminiClient(AiFeatureSettings.transcription("transcription", "gemini", "", TIMEOUT, "minimal"))
                .vertexAI()).isTrue();
    }

    @Test
    @DisplayName("caps Gemini HTTP wait at the feature timeout and disables SDK retries")
    void httpOptions() {
        HttpOptions options = ChatModelFactory.httpOptions(TIMEOUT);

        assertThat(options.timeout()).contains(45_000);
        assertThat(options.retryOptions().orElseThrow().attempts()).contains(1);
        assertThat(ChatModelFactory.noRetry()).isNotNull();
    }

    static ChatModelFactory factory(String geminiKey, String openAiKey) {
        return new ChatModelFactory(
                new StaticListableBeanFactory().getBeanProvider(ObservationRegistry.class),
                geminiKey, "", "", openAiKey, "https://api.openai.com");
    }
}
