package com.undercontroll.infrastructure.ai.config;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.google.genai.common.GoogleGenAiThinkingLevel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorBuilder;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorSettings;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Locale;

@Component
public class ChatModelFactory {

    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    private final ObservationRegistry observationRegistry;
    private final String geminiApiKey;
    private final String geminiProjectId;
    private final String geminiLocation;
    private final String openAiApiKey;
    private final String openAiBaseUrl;

    public ChatModelFactory(
            ObjectProvider<ObservationRegistry> observationRegistry,
            @Value("${undercontroll.ai.gemini.api-key:}") String geminiApiKey,
            @Value("${undercontroll.ai.gemini.project-id:}") String geminiProjectId,
            @Value("${undercontroll.ai.gemini.location:}") String geminiLocation,
            @Value("${undercontroll.ai.openai.api-key:}") String openAiApiKey,
            @Value("${undercontroll.ai.openai.base-url:https://api.openai.com}") String openAiBaseUrl
    ) {
        this.observationRegistry = observationRegistry.getIfAvailable(() -> ObservationRegistry.NOOP);
        this.geminiApiKey = geminiApiKey;
        this.geminiProjectId = geminiProjectId;
        this.geminiLocation = geminiLocation;
        this.openAiApiKey = openAiApiKey;
        this.openAiBaseUrl = openAiBaseUrl;
    }

    public ObservationRegistry observationRegistry() {
        return observationRegistry;
    }

    public ChatModel create(AiFeatureSettings settings) {
        return create(settings, ToolCallingManager.builder().observationRegistry(observationRegistry).build());
    }

    public ChatModel create(AiFeatureSettings settings, ToolCallingManager toolCallingManager) {
        return switch (settings.provider()) {
            case GEMINI -> GoogleGenAiChatModel.builder()
                    .genAiClient(geminiClient(settings))
                    .defaultOptions(geminiOptions(settings))
                    .toolCallingManager(toolCallingManager)
                    .retryTemplate(noRetry())
                    .observationRegistry(observationRegistry)
                    .build();
            case OPENAI -> OpenAiChatModel.builder()
                    .openAiApi(openAiApi(settings))
                    .defaultOptions(openAiOptions(settings))
                    .toolCallingManager(toolCallingManager)
                    .retryTemplate(noRetry())
                    .observationRegistry(observationRegistry)
                    .build();
            case NONE -> throw disabled(settings);
        };
    }

    public Client geminiClient(AiFeatureSettings settings) {
        Client.Builder builder = Client.builder().httpOptions(httpOptions(settings.timeout()));
        if (StringUtils.hasText(geminiApiKey)) {
            return builder.apiKey(geminiApiKey).build();
        }
        if (StringUtils.hasText(geminiProjectId) && StringUtils.hasText(geminiLocation)) {
            return builder.project(geminiProjectId).location(geminiLocation).vertexAI(true).build();
        }
        throw new IllegalStateException("undercontroll.ai." + settings.feature()
                + ".provider=gemini requires GEMINI_API_KEY (or GOOGLE_API_KEY) or undercontroll.ai.gemini.project-id and location");
    }

    public OpenAiAudioApi openAiAudioApi(AiFeatureSettings settings) {
        return OpenAiAudioApi.builder()
                .baseUrl(openAiBaseUrl)
                .apiKey(requireOpenAiKey(settings))
                .restClientBuilder(restClientBuilder(settings.timeout()))
                .webClientBuilder(webClientBuilder(settings.timeout()))
                .build();
    }

    public static RetryTemplate noRetry() {
        return RetryTemplate.builder().maxAttempts(1).build();
    }

    static HttpOptions httpOptions(Duration timeout) {
        return HttpOptions.builder()
                .timeout(Math.toIntExact(timeout.toMillis()))
                .retryOptions(HttpRetryOptions.builder().attempts(1).build())
                .build();
    }

    static GoogleGenAiChatOptions geminiOptions(AiFeatureSettings settings) {
        GoogleGenAiChatOptions.Builder builder = GoogleGenAiChatOptions.builder()
                .model(settings.model())
                .temperature(settings.temperature())
                .maxOutputTokens(settings.maxOutputTokens());
        ThinkingPolicy thinking = settings.thinking();
        if (thinking == ThinkingPolicy.DEFAULT) {
            return builder.build();
        }
        if (settings.capabilities().thinkingControl() == ModelCapabilities.ThinkingControl.BUDGET) {
            builder.thinkingBudget(thinking.budgetTokens());
        } else {
            builder.thinkingLevel(thinking == ThinkingPolicy.HIGH ? GoogleGenAiThinkingLevel.HIGH : GoogleGenAiThinkingLevel.LOW);
        }
        return builder.build();
    }

    static OpenAiChatOptions openAiOptions(AiFeatureSettings settings) {
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder().model(settings.model());
        if (settings.capabilities().thinkingControl() == ModelCapabilities.ThinkingControl.REASONING_EFFORT) {
            builder.maxCompletionTokens(settings.maxOutputTokens());
            if (settings.thinking() != ThinkingPolicy.DEFAULT) {
                builder.reasoningEffort(settings.thinking().name().toLowerCase(Locale.ROOT));
            }
            return builder.build();
        }
        return builder.temperature(settings.temperature())
                .maxTokens(settings.maxOutputTokens())
                .build();
    }

    private OpenAiApi openAiApi(AiFeatureSettings settings) {
        return OpenAiApi.builder()
                .baseUrl(openAiBaseUrl)
                .apiKey(requireOpenAiKey(settings))
                .restClientBuilder(restClientBuilder(settings.timeout()))
                .webClientBuilder(webClientBuilder(settings.timeout()))
                .responseErrorHandler(new DefaultResponseErrorHandler())
                .build();
    }

    private String requireOpenAiKey(AiFeatureSettings settings) {
        if (!StringUtils.hasText(openAiApiKey)) {
            throw new IllegalStateException("undercontroll.ai." + settings.feature()
                    + ".provider=openai requires OPENAI_API_KEY");
        }
        return openAiApiKey;
    }

    static RestClient.Builder restClientBuilder(Duration timeout) {
        return RestClient.builder().requestFactory(ClientHttpRequestFactoryBuilder.detect()
                .build(ClientHttpRequestFactorySettings.defaults().withTimeouts(CONNECT_TIMEOUT, timeout)));
    }

    static WebClient.Builder webClientBuilder(Duration timeout) {
        return WebClient.builder().clientConnector(ClientHttpConnectorBuilder.detect()
                .build(ClientHttpConnectorSettings.defaults().withTimeouts(CONNECT_TIMEOUT, timeout)));
    }

    private static IllegalStateException disabled(AiFeatureSettings settings) {
        return new IllegalStateException("AI feature " + settings.feature() + " is disabled (provider=none)");
    }
}
