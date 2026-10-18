package com.undercontroll.domain.usecase.insights.impl;

import com.undercontroll.domain.gateway.InsightPublicationGateway;
import com.undercontroll.domain.gateway.MarketViewGateway;
import com.undercontroll.domain.model.insight.MarketInsightPublication;
import com.undercontroll.domain.model.insight.InsightsPayload;
import com.undercontroll.domain.usecase.insights.GetMarketInsightsPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
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
    private InsightPublicationGateway insightPublicationGateway;

    @InjectMocks
    private GetMarketInsightsImpl getMarketInsights;

    @Test
    @DisplayName("returns empty output when bucket does not exist")
    void emptyWhenNoBucket() {
        when(marketViewGateway.findCurrentBucketKey()).thenReturn(Optional.empty());

        GetMarketInsightsPort.Output output = getMarketInsights.execute();

        assertNull(output.payload());
        verify(insightPublicationGateway, never()).findLatest();
    }

    @Test
    @DisplayName("returns empty output when the latest publication belongs to another month")
    void emptyWhenNoSuccessfulBatch() {
        when(marketViewGateway.findCurrentBucketKey()).thenReturn(Optional.of("2026-08"));
        when(insightPublicationGateway.findLatest()).thenReturn(Optional.of(publication("2026-07", null)));

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
        when(insightPublicationGateway.findLatest()).thenReturn(Optional.of(publication("2026-08", payload)));

        GetMarketInsightsPort.Output output = getMarketInsights.execute();

        assertSame(payload, output.payload());
        assertEquals("2026-09-01T04:00:00Z", output.generatedAt());
    }

    @Test
    @DisplayName("returns empty output when the stored payload could not be read")
    void emptyOnUnreadablePayload() {
        when(marketViewGateway.findCurrentBucketKey()).thenReturn(Optional.of("2026-08"));
        when(insightPublicationGateway.findLatest()).thenReturn(Optional.of(publication("2026-08", null)));

        assertEquals(GetMarketInsightsPort.Output.empty(), getMarketInsights.execute());
    }

    private static MarketInsightPublication publication(String bucketKey, InsightsPayload payload) {
        return new MarketInsightPublication(1L, 1L, 1L, 10L, bucketKey, null, payload, "gemini", "m", "7",
                Instant.parse("2026-09-01T04:00:00Z"));
    }
}
