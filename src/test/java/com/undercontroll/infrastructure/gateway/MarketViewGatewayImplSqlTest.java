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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketViewGatewayImplSqlTest {

    private static EmbeddedDatabase database;
    private static MarketViewGatewayImpl gateway;

    @BeforeAll
    static void setUp() {
        database = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("market-views;MODE=PostgreSQL")
                .addScript("schema.sql")
                .addScript("market/market-data.sql")
                .build();
        gateway = new MarketViewGatewayImpl(new NamedParameterJdbcTemplate(database));
    }

    @AfterAll
    static void tearDown() {
        database.shutdown();
    }

    @Test
    @DisplayName("bucket comes from the published snapshot and counters only see the published run")
    void countersAndBucket() {
        assertEquals("2026-08", gateway.findCurrentBucketKey().orElseThrow());
        assertEquals(3L, gateway.countCurrentProducts());
        assertEquals(2L, gateway.countDistinctBrands());
    }

    @Test
    @DisplayName("brand rows aggregate the published run")
    void brands() {
        List<MarketBrandSummary> top = gateway.findTopBrands("2026-08", 1);
        List<MarketBrandSummary> momentum = gateway.findBrandMomentum("2026-08");

        assertEquals(1, top.size());
        MarketBrandSummary electrolux = top.getFirst();
        assertEquals("electrolux", electrolux.brandSlug());
        assertEquals(2L, electrolux.productCount());
        assertEquals(1, electrolux.bestRank());
        assertEquals(80.6, electrolux.avgScore());
        assertEquals(1L, electrolux.risingCount());
        assertEquals(List.of("electrolux", "mondial"), momentum.stream().map(MarketBrandSummary::brandSlug).toList());
    }

    @Test
    @DisplayName("category rows expose the average valid price count and filter uncovered domains")
    void categories() {
        MarketCategorySummary top = gateway.findTopCategories("2026-08", 5).getFirst();

        assertEquals("MLB-MICROWAVES", top.domainId());
        assertEquals("Micro-ondas", top.categoryName());
        assertEquals(2L, top.productCount());
        assertEquals(5.0, top.avgValidPriceCount());
        assertEquals(1L, top.risingCount());
        assertEquals(1L, top.highConfidenceCount());
        assertEquals(2, gateway.findCategorySummary("2026-08").size());
        assertEquals(List.of("MLB-MICROWAVES"), gateway.findUncoveredCategories("2026-08", Set.of("MLB-AIR_FRYERS"))
                .stream().map(MarketCategorySummary::domainId).toList());
        assertTrue(gateway.findUncoveredCategories("2026-08", Set.of("MLB-MICROWAVES")).isEmpty());
        assertEquals(1, gateway.findUncoveredCategories("2026-08", Set.of()).size());
    }

    @Test
    @DisplayName("price movements only keep consecutive cohorts with the minimum paired products")
    void movements() {
        List<MarketPriceMovement> movements = gateway.findPriceMovements("2026-08");
        List<MarketPriceMovement> opportunities = gateway.findStockOpportunities("2026-08");

        assertEquals(new MarketPriceMovement("MLB-MICROWAVES", "2026-08", "2026-07", true, 4, 6, 5,
                599.0, 540.0, 10.9), movements.getFirst());
        assertEquals(List.of("MLB-MICROWAVES", "MLB-AIR_FRYERS"),
                movements.stream().map(MarketPriceMovement::domainId).toList());
        assertEquals(List.of("MLB-AIR_FRYERS"), opportunities.stream().map(MarketPriceMovement::domainId).toList());
        assertTrue(gateway.findPriceMovements("2026-07").isEmpty());
    }

    @Test
    @DisplayName("product rows carry the offer sample and dispersion needs three valid prices")
    void products() {
        List<MarketProductCurrent> products = gateway.findAllCurrentProducts();
        MarketProductCurrent microwave = products.stream()
                .filter(product -> product.externalId().equals("MLB1")).findFirst().orElseThrow();

        assertEquals(3, products.size());
        assertNull(microwave.availableOfferCount());
        assertEquals(9, microwave.observedOfferCount());
        assertEquals(8, microwave.validPriceCount());
        assertFalse(microwave.sampleComplete());
        assertEquals(4, microwave.observedSellerCount());
        assertEquals(0.9876, microwave.relevance());
        assertEquals("https://ml/1", microwave.permalink());
        assertEquals(List.of("MLB1", "MLB3"), gateway.findPriceDispersion().stream()
                .map(MarketProductCurrent::externalId).sorted().toList());
    }

    @Test
    @DisplayName("rising products keep only high confidence growth from the published run")
    void rising() {
        List<MarketRisingProduct> rising = gateway.findRisingProductsHighConfidence();

        assertEquals(1, rising.size());
        MarketRisingProduct product = rising.getFirst();
        assertEquals("MLB1", product.externalId());
        assertEquals(579.9, product.priceMedian());
        assertEquals(8, product.validPriceCount());
        assertEquals(9, product.observedOfferCount());
        assertNull(product.availableOfferCount());
        assertFalse(product.sampleComplete());
        assertEquals(4, product.observedSellerCount());
        assertEquals("220V", product.voltage());
        assertEquals(2, product.rankDelta());
    }
}
