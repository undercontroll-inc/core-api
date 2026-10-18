package com.undercontroll.domain.usecase.insights.impl;

import com.undercontroll.domain.enums.InsightGenerationStatus;
import com.undercontroll.domain.gateway.InsightsLlmGateway;
import com.undercontroll.domain.gateway.MarketInsightGateway;
import com.undercontroll.domain.gateway.MarketViewGateway;
import com.undercontroll.domain.exception.LlmOutputException;
import com.undercontroll.domain.insight.InsightContract;
import com.undercontroll.domain.insight.InsightDraftValidator;
import com.undercontroll.domain.insight.InsightInputs;
import com.undercontroll.domain.insight.InsightNarrativeRenderer;
import com.undercontroll.domain.insight.InsightMarketDataLoader;
import com.undercontroll.domain.model.MarketMonthlyInsight;
import com.undercontroll.domain.model.insight.InsightGenerationSettings;
import com.undercontroll.domain.model.insight.InsightMarketData;
import com.undercontroll.domain.model.insight.InsightDraft;
import com.undercontroll.domain.model.insight.InsightDraftRequest;
import com.undercontroll.domain.model.insight.InsightGenerationInput;
import com.undercontroll.domain.model.insight.InsightsPayload;
import com.undercontroll.domain.usecase.insights.GenerateMonthlyInsightsPort;
import com.undercontroll.domain.usecase.insights.InsightGenerationResult;
import com.undercontroll.domain.support.Durations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerateMonthlyInsightsImpl implements GenerateMonthlyInsightsPort {

    private static final String UNKNOWN_ERROR = "unknown error";

    private final MarketViewGateway marketViewGateway;
    private final MarketInsightGateway marketInsightGateway;
    private final InsightMarketDataLoader insightMarketDataLoader;
    private final ObjectProvider<InsightsLlmGateway> insightsLlmGateway;
    private final InsightGenerationSettings insightGenerationSettings;
    private final InsightContract insightContract;

    @Override
    @CacheEvict(value = {"marketInsights", "marketAnalytics"}, allEntries = true)
    public InsightGenerationResult execute(boolean force) {
        Optional<String> currentBucket = marketViewGateway.findCurrentBucketKey();
        if (currentBucket.isEmpty()) {
            log.info("Skipping insight generation: market bucket is not available yet");
            return InsightGenerationResult.noBucket();
        }
        String bucketKey = currentBucket.get();
        Optional<MarketMonthlyInsight> existing = marketInsightGateway.findByBucketKey(bucketKey);
        if (!force && existing.filter(row -> row.getStatus() == InsightGenerationStatus.SUCCESS).isPresent()) {
            log.info("Insights already generated for bucket {}", bucketKey);
            return InsightGenerationResult.alreadyExists(bucketKey);
        }

        InsightsLlmGateway llm = insightsLlmGateway.getIfAvailable();
        if (llm == null) {
            log.warn(
                    "Skipping insight generation: LLM gateway is not available (provider={})",
                    insightGenerationSettings.provider()
            );
            return InsightGenerationResult.noLlm(bucketKey);
        }

        InsightMarketData data = insightMarketDataLoader.load(bucketKey, insightGenerationSettings.repairCatalogDays());
        InsightGenerationInput input = InsightInputs.build(data, marketViewGateway.findAllCurrentProducts(), false);
        String comparison = input.comparisonBucketKey();

        Exception lastError = null;
        long started = System.nanoTime();
        for (int attempt = 1; attempt <= 2; attempt++) {
            long attemptStarted = System.nanoTime();
            try {
                InsightDraft draft = llm.draft(InsightDraftRequest.first(input, insightContract));
                List<String> errors = InsightDraftValidator.validate(draft, input.candidates(), insightContract);
                if (!errors.isEmpty()) {
                    throw new LlmOutputException("Insight draft failed validation: " + String.join("; ", errors));
                }
                InsightsPayload payload = InsightNarrativeRenderer.renderPayload(draft, input);
                persistSuccess(existing.orElse(null), bucketKey, comparison, payload);
                log.info(
                        "Generated monthly insights bucketKey={} attempt={} durationMs={}",
                        bucketKey,
                        attempt,
                        Durations.millisSince(attemptStarted)
                );
                return InsightGenerationResult.success(bucketKey);
            } catch (Exception ex) {
                lastError = ex;
                boolean timeout = isLlmTimeout(ex);
                log.warn(
                        "Insight generation attempt {} failed bucketKey={} timeout={} durationMs={}: {}",
                        attempt,
                        bucketKey,
                        timeout,
                        Durations.millisSince(attemptStarted),
                        ex.getMessage()
                );
                if (timeout) {
                    break;
                }
            }
        }
        persistFailure(existing.orElse(null), bucketKey, comparison, lastError);
        log.warn(
                "Insight generation failed bucketKey={} durationMs={} cause={}",
                bucketKey,
                Durations.millisSince(started),
                lastError == null ? UNKNOWN_ERROR : lastError.getMessage()
        );
        return InsightGenerationResult.failed(
                bucketKey,
                lastError == null ? UNKNOWN_ERROR : lastError.getMessage()
        );
    }

    private void persistSuccess(
            MarketMonthlyInsight existing,
            String bucketKey,
            String comparison,
            InsightsPayload payload
    ) {
        MarketMonthlyInsight insight = base(existing, bucketKey, comparison);
        insight.setStatus(InsightGenerationStatus.SUCCESS);
        insight.setPayload(payload);
        insight.setErrorMessage(null);
        insight.setGeneratedAt(LocalDateTime.now(ZoneOffset.UTC));
        marketInsightGateway.save(insight);
    }

    private void persistFailure(
            MarketMonthlyInsight existing,
            String bucketKey,
            String comparison,
            Exception error
    ) {
        MarketMonthlyInsight insight = base(existing, bucketKey, comparison);
        insight.setStatus(InsightGenerationStatus.FAILED);
        insight.setErrorMessage(error == null ? UNKNOWN_ERROR : truncate(error.getMessage()));
        insight.setGeneratedAt(LocalDateTime.now(ZoneOffset.UTC));
        marketInsightGateway.save(insight);
    }

    private MarketMonthlyInsight base(MarketMonthlyInsight existing, String bucketKey, String comparison) {
        MarketMonthlyInsight insight = existing == null ? new MarketMonthlyInsight() : existing;
        insight.setBucketKey(bucketKey);
        insight.setComparisonBucketKey(comparison);
        insight.setProvider(insightGenerationSettings.provider());
        insight.setModel(insightGenerationSettings.model());
        insight.setPromptVersion(insightGenerationSettings.promptVersion());
        return insight;
    }

    static boolean isLlmTimeout(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof TimeoutException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String lower = message.toLowerCase(Locale.ROOT);
                if (lower.contains("timeout") || lower.contains("timed out") || lower.contains("deadline")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private static String truncate(String message) {
        if (message == null) {
            return UNKNOWN_ERROR;
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
