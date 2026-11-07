package com.undercontroll.live;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.domain.exception.TranscriptionUnavailableException;
import com.undercontroll.domain.insight.InsightContract;
import com.undercontroll.domain.insight.InsightDraftValidator;
import com.undercontroll.domain.insight.InsightInputs;
import com.undercontroll.domain.insight.InsightMarketFixtures;
import com.undercontroll.domain.model.AudioClip;
import com.undercontroll.domain.model.AudioFormat;
import com.undercontroll.domain.model.insight.InsightCandidate;
import com.undercontroll.domain.model.insight.InsightDraft;
import com.undercontroll.domain.model.insight.InsightDraftRequest;
import com.undercontroll.domain.model.insight.InsightGenerationInput;
import com.undercontroll.domain.model.insight.InsightGenerationSettings;
import com.undercontroll.domain.model.insight.LlmFailure;
import com.undercontroll.infrastructure.ai.GeminiAudioTranscriptionAdapter;
import com.undercontroll.infrastructure.ai.LlmErrorClassifier;
import com.undercontroll.infrastructure.ai.OpenAiAudioTranscriptionAdapter;
import com.undercontroll.infrastructure.ai.SpringAiInsightsLlmAdapter;
import com.undercontroll.infrastructure.ai.config.AiFeatureSettings;
import com.undercontroll.infrastructure.ai.config.AiProvider;
import com.undercontroll.infrastructure.ai.config.ChatModelFactory;
import com.undercontroll.infrastructure.metrics.TestMetrics;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("live-ai")
class LiveAiSmokeTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(90);

    private static String provider;
    private static String model;
    private static ChatModelFactory factory;

    @BeforeAll
    static void credentials() {
        provider = env("LIVE_AI_PROVIDER", "gemini");
        model = env("LIVE_AI_MODEL", "");
        String geminiKey = env("GEMINI_API_KEY", env("GOOGLE_API_KEY", ""));
        String openAiKey = env("OPENAI_API_KEY", "");
        assumeTrue(provider.equals("gemini") ? !geminiKey.isBlank() : !openAiKey.isBlank(),
                "Set LIVE_AI_PROVIDER and the matching API key to run the live AI smoke suite");
        factory = new ChatModelFactory(new StaticListableBeanFactory().getBeanProvider(ObservationRegistry.class),
                geminiKey, "", "", openAiKey, env("OPENAI_BASE_URL", "https://api.openai.com"));
    }

    @Test
    @DisplayName("a short chat call finishes normally with text")
    void chat() {
        ChatModel chatModel = factory.create(AiFeatureSettings.chat("ana", provider, model, TIMEOUT, 0.2, 256, "default"));

        ChatResponse response = ChatClient.create(chatModel).prompt()
                .user("Responda apenas com a palavra pronto.")
                .call()
                .chatResponse();

        assertFalse(response.getResult().getOutput().getText().isBlank());
        assertTrue(Set.of("STOP", "FINISH_REASON_UNSPECIFIED")
                .contains(response.getResult().getMetadata().getFinishReason().toUpperCase()));
    }

    @Test
    @DisplayName("the provider accepts the native insight schema and only returns known candidates")
    void insightDraft() throws IOException {
        AiFeatureSettings settings = AiFeatureSettings.chat("insights", provider, model, TIMEOUT, 0.2, 2048, "default");
        ChatModel chatModel = factory.create(settings);
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultSystem(new ClassPathResource("prompts/insights-system-prompt.txt"), StandardCharsets.UTF_8)
                .defaultTemplateRenderer(org.springframework.ai.template.st.StTemplateRenderer.builder()
                        .startDelimiterToken('<').endDelimiterToken('>').build())
                .build();
        SpringAiInsightsLlmAdapter adapter = new SpringAiInsightsLlmAdapter(chatClient, chatModel.getDefaultOptions(),
                new ObjectMapper(), new InsightGenerationSettings(provider, settings.model(), "live", 2, 90, "live"),
                new ClassPathResource("prompts/insights-user-prompt.txt"), TestMetrics.feature("insights"));
        InsightGenerationInput input = InsightInputs.build(InsightMarketFixtures.data(), List.of(), false);
        InsightContract contract = new InsightContract(2);

        InsightDraft draft = adapter.draft(InsightDraftRequest.first(input, contract));

        Set<String> ids = input.candidates().stream().map(InsightCandidate::id).collect(Collectors.toSet());
        assertFalse(draft.insights().isEmpty());
        draft.insights().forEach(item -> assertTrue(ids.contains(item.candidateId())));
        List<String> errors = InsightDraftValidator.validate(draft, input.candidates(), contract);
        if (!errors.isEmpty()) {
            InsightDraft repaired = adapter.draft(InsightDraftRequest.repair(input, contract, draft, errors));
            assertTrue(InsightDraftValidator.validate(repaired, input.candidates(), contract).isEmpty());
        }
    }

    @Test
    @DisplayName("the transcription provider accepts the WAV fixture without auth or configuration errors")
    void transcription() throws IOException {
        AiFeatureSettings settings = AiFeatureSettings.transcription("transcription", provider, "", TIMEOUT, "default");
        AudioClip clip = new AudioClip(new ClassPathResource("audio/note.wav").getContentAsByteArray(), "audio/wav",
                "note.wav", AudioFormat.WAV);
        try {
            String text = settings.provider() == AiProvider.OPENAI
                    ? new OpenAiAudioTranscriptionAdapter(new OpenAiAudioTranscriptionModel(factory.openAiAudioApi(settings),
                    OpenAiAudioTranscriptionAdapter.options(settings.model()), ChatModelFactory.noRetry()),
                    settings.model(), TestMetrics.feature("transcription")).transcribe(clip)
                    : new GeminiAudioTranscriptionAdapter(factory.geminiClient(settings), settings,
                    TestMetrics.feature("transcription")).transcribe(clip);
            assertTrue(text != null);
        } catch (TranscriptionUnavailableException ex) {
            if (ex.getCause() != null) {
                LlmFailure.Kind kind = LlmErrorClassifier.classify(ex.getCause()).kind();
                assertNotEquals(LlmFailure.Kind.AUTH, kind);
                assertNotEquals(LlmFailure.Kind.CONFIG, kind);
            }
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
