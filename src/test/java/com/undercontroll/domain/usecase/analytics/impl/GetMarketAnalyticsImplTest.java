package com.undercontroll.domain.usecase.analytics.impl;

import com.undercontroll.domain.gateway.MarketViewGateway;
import com.undercontroll.domain.model.market.MarketBrandSummary;
import com.undercontroll.domain.model.market.MarketCategorySummary;
import com.undercontroll.domain.usecase.analytics.GetMarketAnalyticsPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMarketAnalyticsImplTest {

    @Mock
    private MarketViewGateway marketViewGateway;

    @InjectMocks
    private GetMarketAnalyticsImpl getMarketAnalytics;

    @Test
    @DisplayName("returns empty analytics when ETL bucket does not exist")
    void emptyWhenNoBucket() {
        when(marketViewGateway.findCurrentBucketKey()).thenReturn(Optional.empty());

        GetMarketAnalyticsPort.Output response = getMarketAnalytics.execute();

        assertNull(response.bucketKey());
        assertEquals(0L, response.totalProducts());
        assertTrue(response.topBrands().isEmpty());
        assertTrue(response.topCategories().isEmpty());
    }

    @Test
    @DisplayName("aggregates current bucket from market views")
    void aggregatesCurrentBucket() {
        when(marketViewGateway.findCurrentBucketKey()).thenReturn(Optional.of("2026-08"));
        when(marketViewGateway.countCurrentProducts()).thenReturn(20L);
        when(marketViewGateway.countDistinctBrands()).thenReturn(8L);
        when(marketViewGateway.findTopBrands("2026-08", 5)).thenReturn(List.of(
                new MarketBrandSummary("mondial", "2026-08", "Mondial", 2L, 2L, 81.3, 1,
                        200.0, 100.0, 300.0, 1.0, 10.0, 1L)
        ));
        when(marketViewGateway.findTopCategories("2026-08", 5)).thenReturn(List.of(
                new MarketCategorySummary("MLB-MICROWAVES", "2026-08", "Micro-ondas", 5L, 3L,
                        77.1, 90.0, 599.0, 500.0, 700.0, 10.0, 11.1, 5.0, 2L, 0L, 4L)
        ));

        GetMarketAnalyticsPort.Output response = getMarketAnalytics.execute();

        assertEquals("2026-08", response.bucketKey());
        assertEquals(20L, response.totalProducts());
        assertEquals(8L, response.brandsAnalyzed());
        assertEquals("Mondial", response.topBrands().get(0).name());
        assertEquals("MLB-MICROWAVES", response.topCategories().get(0).domainId());
    }
}
