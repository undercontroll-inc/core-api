package com.undercontroll.domain.usecase.insights.impl;

import com.undercontroll.domain.enums.InsightGenerationStatus;
import com.undercontroll.domain.gateway.MarketInsightGateway;
import com.undercontroll.domain.gateway.MarketViewGateway;
import com.undercontroll.domain.model.MarketMonthlyInsight;
import com.undercontroll.domain.model.insight.InsightsPayload;
import com.undercontroll.domain.usecase.insights.GetMarketInsightsPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMarketInsightsImplTest {

    @Mock
    private MarketViewGateway marketViewGateway;

    @Mock
    private MarketInsightGateway marketInsightGateway;

    @InjectMocks
    private GetMarketInsightsImpl getMarketInsights;

    @Test
    @DisplayName("returns empty output when bucket does not exist")
    void emptyWhenNoBucket() {
        when(marketViewGateway.findCurrentBucketKey()).thenReturn(Optional.empty());

        GetMarketInsightsPort.Output output = getMarketInsights.execute();

        assertNull(output.payload());
        verify(marketInsightGateway, never()).findSuccessfulByBucketKey(any());
    }

    @Test
    @DisplayName("returns empty output when monthly batch was not generated yet")
    void emptyWhenNoSuccessfulBatch() {
        when(marketViewGateway.findCurrentBucketKey()).thenReturn(Optional.of("2026-08"));
        when(marketInsightGateway.findSuccessfulByBucketKey("2026-08")).thenReturn(Optional.empty());

        assertEquals(GetMarketInsightsPort.Output.empty(), getMarketInsights.execute());
    }

    @Test
    @DisplayName("returns the stored payload and generation time when a successful batch exists")
    void returnsStoredPayload() {
        InsightsPayload payload = new InsightsPayload(
                new InsightsPayload.Periodo("2026-08", "2026-07"),
                new InsightsPayload.CoberturaMatch(1, 0, 0, 0),
                List.of(),
                List.of(),
                List.of("Os dados refletem popularidade e preço anunciado, não volume de vendas.")
        );
        when(marketViewGateway.findCurrentBucketKey()).thenReturn(Optional.of("2026-08"));
        when(marketInsightGateway.findSuccessfulByBucketKey("2026-08")).thenReturn(Optional.of(
                MarketMonthlyInsight.builder()
                        .bucketKey("2026-08")
                        .status(InsightGenerationStatus.SUCCESS)
                        .payload(payload)
                        .generatedAt(LocalDateTime.of(2026, 8, 1, 4, 0))
                        .build()
        ));

        GetMarketInsightsPort.Output output = getMarketInsights.execute();

        assertSame(payload, output.payload());
        assertEquals("2026-08-01T04:00", output.generatedAt());
    }

    @Test
    @DisplayName("returns empty output when the stored payload could not be read")
    void emptyOnUnreadablePayload() {
        when(marketViewGateway.findCurrentBucketKey()).thenReturn(Optional.of("2026-08"));
        when(marketInsightGateway.findSuccessfulByBucketKey("2026-08")).thenReturn(Optional.of(
                MarketMonthlyInsight.builder()
                        .bucketKey("2026-08")
                        .status(InsightGenerationStatus.SUCCESS)
                        .build()
        ));

        assertEquals(GetMarketInsightsPort.Output.empty(), getMarketInsights.execute());
    }
}
