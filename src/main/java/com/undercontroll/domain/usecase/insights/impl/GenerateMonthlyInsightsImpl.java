package com.undercontroll.domain.usecase.insights.impl;

import com.undercontroll.domain.enums.InsightGenerationStatus;
import com.undercontroll.domain.gateway.InsightsLlmGateway;
import com.undercontroll.domain.gateway.MarketInsightGateway;
import com.undercontroll.domain.gateway.MarketViewGateway;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.market.RepairCatalogFactory;
import com.undercontroll.domain.market.RepairCatalogMatcher;
import com.undercontroll.domain.model.MarketMonthlyInsight;
import com.undercontroll.domain.model.insight.InsightPromptContext;
import com.undercontroll.domain.model.insight.InsightsPayload;
import com.undercontroll.domain.model.market.MarketCategorySummary;
import com.undercontroll.domain.model.market.MarketProductCurrent;
import com.undercontroll.domain.model.market.MatchCoverage;
import com.undercontroll.domain.model.market.RepairCatalogItem;
import com.undercontroll.domain.usecase.insights.GenerateMonthlyInsightsPort;
import com.undercontroll.domain.usecase.insights.InsightGenerationResult;
import com.undercontroll.infrastructure.config.InsightsProperties;
import com.undercontroll.domain.support.Durations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
    private final OrderGateway orderGateway;
    private final ObjectProvider<InsightsLlmGateway> insightsLlmGateway;
    private final InsightsProperties insightsProperties;

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
                    insightsProperties.resolvedProvider()
            );
            return InsightGenerationResult.noLlm(bucketKey);
        }

        String comparison = marketViewGateway.findPreviousBucketKey(bucketKey).orElse(null);
        List<MarketCategorySummary> categories = marketViewGateway.findCategorySummary(bucketKey);
        List<RepairCatalogItem> catalog = RepairCatalogFactory.fromRows(
                orderGateway.getRepairCatalog(LocalDate.now(ZoneOffset.UTC).minusDays(insightsProperties.getRepairCatalogDays())),
                categories
        );
        List<MarketProductCurrent> products = marketViewGateway.findAllCurrentProducts();
        MatchCoverage coverage = RepairCatalogMatcher.coverage(products, catalog);
        InsightPromptContext context = new InsightPromptContext(
                bucketKey,
                comparison,
                coverage,
                RepairCatalogMatcher.clientDomainIds(catalog),
                catalog,
                categories
        );

        Exception lastError = null;
        long started = System.nanoTime();
        for (int attempt = 1; attempt <= 2; attempt++) {
            long attemptStarted = System.nanoTime();
            try {
                InsightsPayload payload = llm.generate(context);
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
        insight.setProvider(insightsProperties.resolvedProvider());
        insight.setModel(insightsProperties.activeModel());
        insight.setPromptVersion(insightsProperties.getPromptVersion());
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
