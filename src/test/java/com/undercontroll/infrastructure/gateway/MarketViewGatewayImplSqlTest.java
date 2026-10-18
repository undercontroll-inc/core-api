package com.undercontroll.infrastructure.gateway;

import com.undercontroll.domain.model.market.MarketBrandSummary;
import com.undercontroll.domain.model.market.MarketCategorySummary;
import com.undercontroll.domain.model.market.MarketPriceMovement;
import com.undercontroll.domain.model.market.MarketProductCurrent;
import com.undercontroll.domain.model.market.MarketRisingProduct;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MarketViewGatewayImplSqlTest {

    private static EmbeddedDatabase database;
    private static MarketViewGatewayImpl gateway;

    @BeforeAll
    static void setUp() {
        database = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("market-views;MODE=PostgreSQL")
                .addScript("market/market-views.sql")
                .build();
        gateway = new MarketViewGatewayImpl(new NamedParameterJdbcTemplate(database));
    }

    @AfterAll
    static void tearDown() {
        database.shutdown();
    }

    @Test
    @DisplayName("bucket and counters read the current product view")
    void countersAndBucket() {
        assertEquals("2026-08", gateway.findCurrentBucketKey().orElseThrow());
        assertEquals("2026-07", gateway.findPreviousBucketKey("2026-08").orElseThrow());
        assertEquals(1L, gateway.countCurrentProducts());
        assertEquals(1L, gateway.countDistinctBrands());
    }

    @Test
    @DisplayName("brand rows map every column, including nulls and numeric conversions")
    void brands() {
        List<MarketBrandSummary> top = gateway.findTopBrands("2026-08", 1);
        List<MarketBrandSummary> momentum = gateway.findBrandMomentum("2026-08");

        assertEquals(new MarketBrandSummary("electrolux", "2026-08", "Electrolux", 10L, 2L, 80.5, 1, 599.9,
                400.0, 800.0, 2.5, 5.0, 3L), top.getFirst());
        assertEquals(1, top.size());
        assertEquals("mondial", momentum.getFirst().brandSlug());
        assertNull(momentum.getFirst().avgPriceDeltaPct());
    }

    @Test
    @DisplayName("category rows alias the sample name and filter uncovered domains")
    void categories() {
        MarketCategorySummary top = gateway.findTopCategories("2026-08", 5).getFirst();

        assertEquals(new MarketCategorySummary("MLB-MICROWAVES", "2026-08", "Micro-ondas", 12L, 4L, 70.0, 90.0,
                599.0, 400.0, 800.0, 6.0, 1.0, 4.0, 2L, 1L, 2L), top);
        assertEquals(2, gateway.findCategorySummary("2026-08").size());
        assertEquals(List.of("MLB-AIR_FRYERS"), gateway.findUncoveredCategories("2026-08", Set.of("MLB-MICROWAVES"))
                .stream().map(MarketCategorySummary::domainId).toList());
        assertEquals(2, gateway.findUncoveredCategories("2026-08", Set.of()).size());
    }

    @Test
    @DisplayName("price movement queries keep their filters and ordering")
    void movements() {
        List<MarketPriceMovement> movements = gateway.findPriceMovements("2026-08");
        List<MarketPriceMovement> opportunities = gateway.findStockOpportunities("2026-08");

        assertEquals(new MarketPriceMovement("MLB-MICROWAVES", "2026-08", "2026-07", 4L, 10L, 3L, 599.0, 540.0, 10.9, 2.0),
                movements.getFirst());
        assertEquals(2, movements.size());
        assertEquals(List.of("MLB-AIR_FRYERS"), opportunities.stream().map(MarketPriceMovement::domainId).toList());
    }

    @Test
    @DisplayName("product and rising rows map all columns")
    void products() {
        MarketProductCurrent product = gateway.findAllCurrentProducts().getFirst();
        MarketRisingProduct rising = gateway.findRisingProductsHighConfidence().getFirst();

        assertEquals("micro-ondas 20l", product.canonicalTitle());
        assertEquals(1200, product.powerWatts());
        assertEquals(0.9876, product.relevance());
        assertEquals(9, product.offerCount());
        assertEquals("https://ml/1", product.permalink());
        assertEquals(1, gateway.findPriceDispersion().size());
        assertEquals(579.9, rising.priceMedian());
        assertEquals("220V", rising.voltage());
        assertEquals(2, rising.rankDelta());
    }
}
