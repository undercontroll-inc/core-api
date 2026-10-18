package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.domain.exception.InsightLlmException;
import com.undercontroll.domain.exception.LlmOutputException;
import com.undercontroll.domain.gateway.InsightsLlmGateway;
import com.undercontroll.domain.insight.InsightContract;
import com.undercontroll.domain.model.insight.InsightCandidate;
import com.undercontroll.domain.model.insight.InsightDraft;
import com.undercontroll.domain.model.insight.InsightDraftRequest;
import com.undercontroll.domain.model.insight.InsightGenerationInput;
import com.undercontroll.domain.model.insight.InsightGenerationSettings;
import com.undercontroll.domain.model.insight.InsightMonthLabel;
import com.undercontroll.domain.model.insight.LlmFailure;
import com.undercontroll.domain.support.Durations;
import com.undercontroll.infrastructure.metrics.FeatureMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.StructuredOutputChatOptions;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
public class SpringAiInsightsLlmAdapter implements InsightsLlmGateway {

    private static final Set<String> TRUNCATED = Set.of("MAX_TOKENS", "LENGTH");

    private final ChatClient chatClient;
    private final ChatOptions defaultOptions;
    private final ObjectMapper objectMapper;
    private final InsightGenerationSettings settings;
    private final Resource userPrompt;
    private final FeatureMetrics metrics;

    public SpringAiInsightsLlmAdapter(
            ChatClient chatClient,
            ChatOptions defaultOptions,
            ObjectMapper objectMapper,
            InsightGenerationSettings settings,
            Resource userPrompt,
            FeatureMetrics metrics
    ) {
        if (!(defaultOptions instanceof StructuredOutputChatOptions)) {
            throw new IllegalStateException("undercontroll.ai.insights.provider=" + settings.provider()
                    + " does not support native structured output");
        }
        this.chatClient = chatClient;
        this.defaultOptions = defaultOptions;
        this.objectMapper = objectMapper;
        this.settings = settings;
        this.userPrompt = userPrompt;
        this.metrics = metrics;
    }

    @Override
    public InsightDraft draft(InsightDraftRequest request) {
        InsightGenerationInput input = request.input();
        if (input.candidates().isEmpty()) {
            return new InsightDraft(List.of());
        }
        long started = System.nanoTime();
        try {
            return requestDraft(request, started);
        } catch (InsightLlmException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            LlmFailure failure = LlmErrorClassifier.classify(ex);
            metrics.record(failure.kind().name(), null, started);
            log.warn("Insight draft failed bucketKey={} kind={} status={} durationMs={}",
                    input.bucketKey(), failure.kind(), failure.httpStatus(), Durations.millisSince(started));
            throw new InsightLlmException(failure, failure.kind() + ": " + ex.getMessage(), ex);
        }
    }

    private InsightDraft requestDraft(InsightDraftRequest request, long started) {
        InsightGenerationInput input = request.input();
        ChatResponse response = chatClient.prompt()
                .options(structuredOptions(request))
                .user(user -> user.text(userPrompt, StandardCharsets.UTF_8).params(params(request)))
                .call()
                .chatResponse();
        String finishReason = finishReason(response);
        log.info("Insight draft bucketKey={} repair={} provider={} model={} finishReason={} durationMs={}",
                input.bucketKey(), request.repairing(), settings.provider(), settings.model(), finishReason,
                Durations.millisSince(started));
        if (finishReason != null && TRUNCATED.contains(finishReason.toUpperCase(Locale.ROOT))) {
            metrics.record(LlmFailure.Kind.INVALID_OUTPUT.name(), finishReason, started);
            throw new InsightLlmException(LlmFailure.of(LlmFailure.Kind.INVALID_OUTPUT),
                    "Insight draft was truncated (finish reason " + finishReason + ")", null);
        }
        String content = response == null || response.getResult() == null
                ? null
                : response.getResult().getOutput().getText();
        try {
            if (content == null || content.isBlank()) {
                throw new LlmOutputException("Insight draft is empty");
            }
            InsightDraft draft = objectMapper.readValue(content, InsightDraftJson.class).toDomain();
            metrics.record("success", finishReason, started);
            return draft;
        } catch (JsonProcessingException | LlmOutputException ex) {
            metrics.record(LlmFailure.Kind.INVALID_OUTPUT.name(), finishReason, started);
            throw new InsightLlmException(LlmFailure.of(LlmFailure.Kind.INVALID_OUTPUT),
                    ex instanceof LlmOutputException ? ex.getMessage()
                            : "Insight draft is not valid JSON for " + InsightContract.VERSION, ex);
        }
    }

    ChatOptions structuredOptions(InsightDraftRequest request) {
        ChatOptions options = defaultOptions.copy();
        List<String> ids = request.input().candidates().stream().map(InsightCandidate::id).toList();
        ((StructuredOutputChatOptions) options).setOutputSchema(
                InsightDraftSchema.json(objectMapper, request.contract(), ids));
        return options;
    }

    Map<String, Object> params(InsightDraftRequest request) {
        InsightGenerationInput input = request.input();
        Map<String, Object> params = new HashMap<>();
        params.put("mesAtual", InsightMonthLabel.of(input.bucketKey()));
        params.put("maxInsights", request.contract().maxInsights());
        params.put("candidatos", json(input.candidates().stream().map(InsightCandidateJson::from).toList()));
        params.put("correcao", request.repairing() ? correction(request) : "");
        return params;
    }

    private String correction(InsightDraftRequest request) {
        return "CORREÇÃO OBRIGATÓRIA\n"
                + "Sua resposta anterior foi rejeitada. Corrija todos os erros abaixo e devolva o JSON completo.\n"
                + "Resposta anterior: " + json(InsightDraftJson.from(request.previousDraft())) + "\n"
                + "Erros:\n- " + String.join("\n- ", request.errors()) + "\n";
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize insight prompt data", ex);
        }
    }

    private static String finishReason(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getMetadata() == null) {
            return null;
        }
        return response.getResult().getMetadata().getFinishReason();
    }
}
