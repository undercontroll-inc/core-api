package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.domain.gateway.InsightsLlmGateway;
import com.undercontroll.domain.model.insight.InsightCandidate;
import com.undercontroll.domain.model.insight.InsightMonthLabel;
import com.undercontroll.domain.model.insight.InsightGenerationSettings;
import com.undercontroll.domain.model.insight.InsightPromptContext;
import com.undercontroll.domain.model.insight.InsightsPayload;
import com.undercontroll.infrastructure.mapper.InsightsPayloadJsonMapper;
import com.undercontroll.domain.support.Durations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class SpringAiInsightsLlmAdapter implements InsightsLlmGateway {

    private final ChatClient insightsChatClient;
    private final ObjectMapper objectMapper;
    private final InsightPayloadValidator insightPayloadValidator;
    private final InsightGenerationSettings settings;
    private final Resource userPrompt;
    private final InsightsPayloadJsonMapper payloadMapper;

    public SpringAiInsightsLlmAdapter(
            ChatClient insightsChatClient,
            ObjectMapper objectMapper,
            InsightPayloadValidator insightPayloadValidator,
            InsightGenerationSettings settings,
            Resource userPrompt,
            InsightsPayloadJsonMapper payloadMapper
    ) {
        this.insightsChatClient = insightsChatClient;
        this.objectMapper = objectMapper;
        this.insightPayloadValidator = insightPayloadValidator;
        this.settings = settings;
        this.userPrompt = userPrompt;
        this.payloadMapper = payloadMapper;
    }

    @Override
    public InsightsPayload generate(InsightPromptContext context) {
        long started = System.nanoTime();
        String bucketKey = context.bucketKey();
        log.info(
                "Generating insights bucketKey={} comparison={} provider={} model={}",
                bucketKey,
                context.comparisonBucketKey(),
                settings.provider(),
                settings.model()
        );
        try {
            InsightsPayloadJson json = insightsChatClient.prompt()
                    .user(user -> user.text(userPrompt, StandardCharsets.UTF_8)
                            .param("mesAtual", InsightMonthLabel.of(context.bucketKey()))
                            .param("mesComparacao", InsightMonthLabel.of(context.comparisonBucketKey()))
                            .param("bucketAtual", context.bucketKey())
                            .param("bucketComparacao", context.comparisonBucketKey() == null ? "null" : context.comparisonBucketKey())
                            .param("maxInsights", settings.maxInsights())
                            .param("grounding", groundingJson(context)))
                    .call()
                    .entity(InsightsPayloadJson.class);
            if (json == null) {
                throw new IllegalStateException("LLM returned an empty insights payload");
            }
            InsightsPayload payload = payloadMapper.toDomain(json);
            insightPayloadValidator.validate(payload, context);
            log.info("Generated insights bucketKey={} durationMs={}", bucketKey, Durations.millisSince(started));
            return bind(payload, context);
        } catch (RuntimeException ex) {
            log.warn(
                    "Insight generation failed bucketKey={} durationMs={} cause={}",
                    bucketKey,
                    Durations.millisSince(started),
                    ex.toString()
            );
            throw ex;
        }
    }

    private String groundingJson(InsightPromptContext context) {
        Map<String, Object> grounding = new LinkedHashMap<>();
        grounding.put("cobertura_match", InsightsPayloadJson.CoberturaMatch.from(context.coverage()));
        grounding.put("candidatos", context.candidates().stream().map(InsightCandidateJson::from).toList());
        try {
            return objectMapper.writeValueAsString(grounding);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize insight grounding", ex);
        }
    }

    static InsightsPayload bind(InsightsPayload payload, InsightPromptContext context) {
        List<InsightsPayload.InsightItem> insights = payload.insights().stream()
                .map(item -> {
                    InsightCandidate candidate = context.candidate(item.candidateId()).orElseThrow();
                    return new InsightsPayload.InsightItem(
                            candidate.id(),
                            item.texto(),
                            candidate.type().name(),
                            candidate.domainId(),
                            candidate.categoryName(),
                            new InsightsPayload.Evidencia(candidate.evidenceView(), candidate.evidenceFields()),
                            candidate.confidence(),
                            candidate.repairRelation().name(),
                            item.acaoSugerida()
                    );
                })
                .toList();
        return new InsightsPayload(payload.periodo(), payload.coberturaMatch(), insights, payload.produtosEmAlta(),
                payload.limitacoes());
    }
}
