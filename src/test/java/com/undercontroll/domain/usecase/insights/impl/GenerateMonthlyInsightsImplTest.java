package com.undercontroll.domain.usecase.insights.impl;

import com.undercontroll.domain.enums.InsightGenerationStatus;
import com.undercontroll.domain.gateway.InsightsLlmGateway;
import com.undercontroll.domain.gateway.MarketInsightGateway;
import com.undercontroll.domain.gateway.MarketViewGateway;
import com.undercontroll.domain.insight.InsightMarketDataLoader;
import com.undercontroll.domain.model.insight.InsightMarketData;
import com.undercontroll.domain.model.market.RepairCatalogItem;
import com.undercontroll.domain.model.MarketMonthlyInsight;
import com.undercontroll.domain.insight.InsightContract;
import com.undercontroll.domain.model.insight.InsightDraft;
import com.undercontroll.domain.model.insight.InsightDraftRequest;
import com.undercontroll.domain.model.market.MarketPriceMovement;
import com.undercontroll.domain.usecase.insights.InsightGenerationResult;
import com.undercontroll.domain.model.insight.InsightGenerationSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateMonthlyInsightsImplTest {

    @Mock
    private MarketViewGateway marketViewGateway;

    @Mock
    private MarketInsightGateway marketInsightGateway;

    @Mock
    private InsightMarketDataLoader insightMarketDataLoader;

    @Mock
    private ObjectProvider<InsightsLlmGateway> insightsLlmGateway;

    @Mock
    private InsightsLlmGateway llmGateway;

    private final InsightGenerationSettings settings =
            new InsightGenerationSettings("openai", "gpt-4.1-mini", "1", 7, 90);
    private GenerateMonthlyInsightsImpl generateMonthlyInsights;

    @BeforeEach
    void setUp() {
        generateMonthlyInsights = new GenerateMonthlyInsightsImpl(
                marketViewGateway,
                marketInsightGateway,
                insightMarketDataLoader,
                insightsLlmGateway,
                settings,
                new InsightContract(7)
        );
    }

    @Test
    @DisplayName("does not call the LLM when the ETL bucket is missing")
    void skipsWhenNoBucket() {
        when(marketViewGateway.findCurrentBucketKey()).thenReturn(Optional.empty());

        generateMonthlyInsights.execute(false);

        verify(insightsLlmGateway, never()).getIfAvailable();
        verify(marketInsightGateway, never()).save(any());
    }

    @Test
    @DisplayName("does not call the LLM when a SUCCESS batch already exists")
    void skipsWhenAlreadyGenerated() {
        when(marketViewGateway.findCurrentBucketKey()).thenReturn(Optional.of("2026-08"));
        when(marketInsightGateway.findByBucketKey("2026-08")).thenReturn(Optional.of(
                MarketMonthlyInsight.builder()
                        .bucketKey("2026-08")
                        .status(InsightGenerationStatus.SUCCESS)
                        .build()
        ));

        generateMonthlyInsights.execute(false);

        verify(insightsLlmGateway, never()).getIfAvailable();
        verify(llmGateway, never()).draft(any());
    }

    @Test
    @DisplayName("force=true regenerates even when a SUCCESS batch already exists")
    void forceOverwritesExistingSuccess() {
        when(marketViewGateway.findCurrentBucketKey()).thenReturn(Optional.of("2026-08"));
        when(marketInsightGateway.findByBucketKey("2026-08")).thenReturn(Optional.of(
                MarketMonthlyInsight.builder()
                        .bucketKey("2026-08")
                        .status(InsightGenerationStatus.SUCCESS)
                        .build()
        ));
        stubSuccessfulLlm("2026-08");

        InsightGenerationResult result = generateMonthlyInsights.execute(true);

        ArgumentCaptor<MarketMonthlyInsight> captor = ArgumentCaptor.forClass(MarketMonthlyInsight.class);
        verify(marketInsightGateway).save(captor.capture());
        assertEquals(InsightGenerationStatus.SUCCESS, captor.getValue().getStatus());
        assertEquals(InsightGenerationResult.Status.SUCCESS, result.status());
        assertEquals("2026-08", result.bucketKey());
    }

    @Test
    @DisplayName("skips generation when the LLM bean is not configured")
    void skipsWhenLlmMissing() {
        when(marketViewGateway.findCurrentBucketKey()).thenReturn(Optional.of("2026-08"));
        when(marketInsightGateway.findByBucketKey("2026-08")).thenReturn(Optional.empty());
        when(insightsLlmGateway.getIfAvailable()).thenReturn(null);

        generateMonthlyInsights.execute(false);

        verify(llmGateway, never()).draft(any());
        verify(marketInsightGateway, never()).save(any());
    }

    @Test
    @DisplayName("persists SUCCESS payload when the LLM returns a valid batch")
    void persistsSuccessfulGeneration() {
        when(marketViewGateway.findCurrentBucketKey()).thenReturn(Optional.of("2026-08"));
        when(marketInsightGateway.findByBucketKey("2026-08")).thenReturn(Optional.empty());
        stubSuccessfulLlm("2026-08");

        generateMonthlyInsights.execute(false);

        ArgumentCaptor<MarketMonthlyInsight> captor = ArgumentCaptor.forClass(MarketMonthlyInsight.class);
        verify(marketInsightGateway).save(captor.capture());
        assertEquals(InsightGenerationStatus.SUCCESS, captor.getValue().getStatus());
        assertEquals("2026-08", captor.getValue().getBucketKey());
        assertEquals("openai", captor.getValue().getProvider());
        assertEquals("gpt-4.1-mini", captor.getValue().getModel());
        assertEquals("1", captor.getValue().getPromptVersion());

        ArgumentCaptor<InsightDraftRequest> contextCaptor = ArgumentCaptor.forClass(InsightDraftRequest.class);
        verify(llmGateway).draft(contextCaptor.capture());
        assertEquals("2026-07", contextCaptor.getValue().input().comparisonBucketKey());
        assertEquals(List.of("ALERTA_PRECO:MLB-MICROWAVES"), contextCaptor.getValue().input().candidates().stream()
                .filter(candidate -> candidate.type().name().equals("ALERTA_PRECO"))
                .map(candidate -> candidate.id())
                .toList());
    }

    @Test
    @DisplayName("persists FAILED after two unsuccessful LLM attempts")
    void persistsFailureAfterRetry() {
        when(marketViewGateway.findCurrentBucketKey()).thenReturn(Optional.of("2026-08"));
        when(marketInsightGateway.findByBucketKey("2026-08")).thenReturn(Optional.empty());
        when(insightsLlmGateway.getIfAvailable()).thenReturn(llmGateway);
        when(insightMarketDataLoader.load("2026-08", 90)).thenReturn(emptyData("2026-08"));
        when(marketViewGateway.findAllCurrentProducts()).thenReturn(List.of());
        when(llmGateway.draft(any())).thenThrow(new IllegalStateException("model error"));

        generateMonthlyInsights.execute(false);

        verify(llmGateway, times(2)).draft(any());
        ArgumentCaptor<MarketMonthlyInsight> captor = ArgumentCaptor.forClass(MarketMonthlyInsight.class);
        verify(marketInsightGateway).save(captor.capture());
        assertEquals(InsightGenerationStatus.FAILED, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("does not retry the LLM after a timeout")
    void doesNotRetryTimeout() {
        when(marketViewGateway.findCurrentBucketKey()).thenReturn(Optional.of("2026-08"));
        when(marketInsightGateway.findByBucketKey("2026-08")).thenReturn(Optional.empty());
        when(insightsLlmGateway.getIfAvailable()).thenReturn(llmGateway);
        when(insightMarketDataLoader.load("2026-08", 90)).thenReturn(emptyData("2026-08"));
        when(marketViewGateway.findAllCurrentProducts()).thenReturn(List.of());
        when(llmGateway.draft(any())).thenThrow(new IllegalStateException(
                "Failed to generate content", new TimeoutException("timeout")));

        generateMonthlyInsights.execute(false);

        verify(llmGateway, times(1)).draft(any());
        ArgumentCaptor<MarketMonthlyInsight> captor = ArgumentCaptor.forClass(MarketMonthlyInsight.class);
        verify(marketInsightGateway).save(captor.capture());
        assertEquals(InsightGenerationStatus.FAILED, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("detects Gemini timeout messages in the cause chain")
    void detectsTimeouts() {
        assertTrue(GenerateMonthlyInsightsImpl.isLlmTimeout(new TimeoutException("x")));
        assertTrue(GenerateMonthlyInsightsImpl.isLlmTimeout(
                new IllegalStateException("Failed to generate content", new RuntimeException("deadline exceeded"))));
        assertFalse(GenerateMonthlyInsightsImpl.isLlmTimeout(new IllegalStateException("model error")));
    }

    private void stubSuccessfulLlm(String bucketKey) {
        when(insightsLlmGateway.getIfAvailable()).thenReturn(llmGateway);
        when(insightMarketDataLoader.load(bucketKey, 90)).thenReturn(new InsightMarketData(
                bucketKey,
                List.of(new RepairCatalogItem("Electrolux", "ME23S", "MICROONDAS", 12L, "electrolux",
                        "electrolux:me23s", "MLB-MICROWAVES")),
                List.of(),
                List.of(new MarketPriceMovement("MLB-MICROWAVES", bucketKey, "2026-07", true, 4, 6, 5, 599.0, 540.0, 10.9)),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        ));
        when(marketViewGateway.findAllCurrentProducts()).thenReturn(List.of());
        when(llmGateway.draft(any())).thenReturn(new InsightDraft(List.of()));
    }

    private static InsightMarketData emptyData(String bucketKey) {
        return new InsightMarketData(bucketKey, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of());
    }
}
