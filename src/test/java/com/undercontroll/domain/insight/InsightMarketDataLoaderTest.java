package com.undercontroll.domain.insight;

import com.undercontroll.domain.gateway.MarketViewGateway;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.model.insight.InsightMarketData;
import com.undercontroll.domain.model.market.MarketCategorySummary;
import com.undercontroll.domain.model.market.RepairCatalogRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InsightMarketDataLoaderTest {

    private final MarketViewGateway marketViewGateway = mock(MarketViewGateway.class);
    private final OrderGateway orderGateway = mock(OrderGateway.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-02T10:00:00Z"), ZoneOffset.UTC);
    private final InsightMarketDataLoader loader = new InsightMarketDataLoader(marketViewGateway, orderGateway, clock);

    @Test
    @DisplayName("loads the published views and the recent repair catalog mapped to market categories")
    void loads() {
        MarketCategorySummary microwaves = InsightMarketFixtures.category("MLB-MICROWAVES", "Micro-ondas", 5L, 1L, 1L);
        when(marketViewGateway.findCategorySummary("2026-08")).thenReturn(List.of(microwaves));
        when(orderGateway.getRepairCatalog(LocalDate.parse("2026-06-04")))
                .thenReturn(List.of(new RepairCatalogRow("Electrolux", "ME23S", "Microondas", 7L)));

        InsightMarketData data = loader.load("2026-08", 90);

        assertEquals("2026-08", data.bucketKey());
        assertEquals(List.of(microwaves), data.categories());
        assertEquals("MLB-MICROWAVES", data.catalog().getFirst().domainId());
        assertEquals("electrolux:me23s", data.catalog().getFirst().productKey());
        verify(marketViewGateway).findUncoveredCategories("2026-08", Set.of("MLB-MICROWAVES"));
        verify(marketViewGateway).findPriceMovements("2026-08");
        verify(marketViewGateway).findStockOpportunities("2026-08");
        verify(marketViewGateway).findBrandMomentum("2026-08");
        verify(marketViewGateway).findRisingProductsHighConfidence();
        verify(marketViewGateway).findPriceDispersion();
    }
}
