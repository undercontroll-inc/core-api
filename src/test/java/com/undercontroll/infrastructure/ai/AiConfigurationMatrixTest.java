package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.domain.gateway.AnaChatGateway;
import com.undercontroll.domain.gateway.AudioTranscriptionGateway;
import com.undercontroll.domain.gateway.InsightsLlmGateway;
import com.undercontroll.domain.model.insight.InsightGenerationSettings;
import com.undercontroll.domain.model.insight.InsightJobSettings;
import com.undercontroll.infrastructure.ai.config.AiFeatureSettings;
import com.undercontroll.infrastructure.ai.config.AiProvider;
import com.undercontroll.infrastructure.ai.config.ChatModelFactory;
import com.undercontroll.infrastructure.mapper.InsightsPayloadJsonMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AiConfigurationMatrixTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(AiConfigurationMatrixTest::bootConversions)
            .withUserConfiguration(AnaAiConfig.class, InsightsAiConfig.class, TranscriptionAiConfig.class)
            .withBean(ChatModelFactory.class)
            .withBean(RestClient.Builder.class, RestClient::builder)
            .withBean(AnaShopTools.class, () -> mock(AnaShopTools.class))
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(InsightsPayloadJsonMapper.class, () -> mock(InsightsPayloadJsonMapper.class))
            .withPropertyValues(
                    "undercontroll.ai.gemini.api-key=test-gemini",
                    "undercontroll.ai.openai.api-key=test-openai"
            );

    @Test
    @DisplayName("provider none disables every AI feature but keeps insight settings for auditing")
    void allDisabled() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(AnaChatGateway.class);
            assertThat(context).doesNotHaveBean(InsightsLlmGateway.class);
            assertThat(context).doesNotHaveBean(AudioTranscriptionGateway.class);
            assertThat(context.getBean(InsightGenerationSettings.class).provider()).isEqualTo("none");
        });
    }

    @Test
    @DisplayName("each feature resolves its own provider and model")
    void independentProviders() {
        runner.withPropertyValues(
                "undercontroll.ai.ana.provider=openai",
                "undercontroll.ai.insights.provider=gemini",
                "undercontroll.ai.insights.model=gemini-3.8-flash",
                "undercontroll.ai.transcription.provider=gemini"
        ).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(AnaChatGateway.class);
            assertThat(context).hasSingleBean(InsightsLlmGateway.class);
            assertThat(context.getBean("anaAiSettings", AiFeatureSettings.class).provider()).isEqualTo(AiProvider.OPENAI);
            assertThat(context.getBean("anaAiSettings", AiFeatureSettings.class).model()).isEqualTo("gpt-4.1-mini");
            InsightGenerationSettings insights = context.getBean(InsightGenerationSettings.class);
            assertThat(insights.provider()).isEqualTo("gemini");
            assertThat(insights.model()).isEqualTo("gemini-3.8-flash");
            assertThat(context.getBean(AudioTranscriptionGateway.class)).isInstanceOf(GeminiAudioTranscriptionAdapter.class);
        });
    }

    @Test
    @DisplayName("switching insights provider leaves Ana and transcription untouched")
    void insightsSwitchIsIsolated() {
        runner.withPropertyValues(
                "undercontroll.ai.ana.provider=gemini",
                "undercontroll.ai.insights.provider=openai",
                "undercontroll.ai.transcription.provider=openai"
        ).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean("anaAiSettings", AiFeatureSettings.class).model()).isEqualTo("gemini-3.6-flash");
            assertThat(context.getBean(InsightGenerationSettings.class).model()).isEqualTo("gpt-4.1-mini");
            assertThat(context.getBean(AudioTranscriptionGateway.class)).isInstanceOf(OpenAiAudioTranscriptionAdapter.class);
        });
        runner.withPropertyValues("undercontroll.ai.insights.provider=gemini").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(AnaChatGateway.class);
            assertThat(context).doesNotHaveBean(AudioTranscriptionGateway.class);
            assertThat(context).hasSingleBean(InsightsLlmGateway.class);
        });
    }

    @Test
    @DisplayName("the insight model config hash changes with the model and keeps job settings valid")
    void modelConfigHash() {
        runner.withPropertyValues("undercontroll.ai.insights.provider=gemini").run(first ->
                runner.withPropertyValues(
                        "undercontroll.ai.insights.provider=gemini",
                        "undercontroll.ai.insights.model=gemini-3.8-flash"
                ).run(second -> {
                    String original = first.getBean(InsightGenerationSettings.class).modelConfigHash();
                    assertThat(original).hasSize(64);
                    assertThat(original).isNotEqualTo(second.getBean(InsightGenerationSettings.class).modelConfigHash());
                    assertThat(first.getBean(InsightJobSettings.class).leaseDuration()).isEqualTo(Duration.ofMinutes(10));
                }));
        runner.withPropertyValues("undercontroll.ai.insights.lease-duration=1m").run(context ->
                assertThat(context.getStartupFailure()).hasStackTraceContaining("lease-duration"));
    }

    @Test
    @DisplayName("typo in a provider fails startup naming the key")
    void invalidProvider() {
        runner.withPropertyValues("undercontroll.ai.ana.provider=gemni").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("undercontroll.ai.ana.provider='gemni'");
        });
    }

    @Test
    @DisplayName("missing credential fails startup naming the feature")
    void missingCredential() {
        new ApplicationContextRunner()
                .withInitializer(AiConfigurationMatrixTest::bootConversions)
                .withUserConfiguration(TranscriptionAiConfig.class)
                .withBean(ChatModelFactory.class)
                .withPropertyValues("undercontroll.ai.transcription.provider=openai")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("undercontroll.ai.transcription.provider=openai requires OPENAI_API_KEY");
                });
    }

    @Test
    @DisplayName("unsupported thinking level fails startup")
    void unsupportedThinking() {
        runner.withPropertyValues(
                "undercontroll.ai.insights.provider=gemini",
                "undercontroll.ai.insights.thinking=off"
        ).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("undercontroll.ai.insights.thinking=off");
        });
    }

    private static void bootConversions(ConfigurableApplicationContext context) {
        context.getBeanFactory().setConversionService(ApplicationConversionService.getSharedInstance());
    }
}
