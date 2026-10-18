package com.undercontroll.domain.insight;

import com.undercontroll.domain.model.insight.InsightMarketData;
import com.undercontroll.domain.model.market.MarketBrandSummary;
import com.undercontroll.domain.model.market.MarketCategorySummary;
import com.undercontroll.domain.model.market.MarketPriceMovement;
import com.undercontroll.domain.model.market.MarketProductCurrent;
import com.undercontroll.domain.model.market.MarketRisingProduct;
import com.undercontroll.domain.model.market.RepairCatalogItem;

import java.util.List;

public final class InsightMarketFixtures {

    public static final String BUCKET = "2026-08";

    private InsightMarketFixtures() {
    }

    public static InsightMarketData data() {
        return new InsightMarketData(
                BUCKET,
                List.of(
                        repair("Electrolux", "ME23S", 12, "MLB-MICROWAVES"),
                        repair("Mondial", "AF14", 4, "MLB-AIR_FRYERS")
                ),
                List.of(
                        category("MLB-MICROWAVES", "Micro-ondas", 12L, 2L, 1L),
                        category("MLB-AIR_FRYERS", "Air fryer", 8L, 1L, 0L),
                        category("MLB-FANS", "Ventilador", 5L, 3L, 2L)
                ),
                List.of(
                        movement("MLB-MICROWAVES", "2026-07", true, 6, 10.9),
                        movement("MLB-AIR_FRYERS", "2026-07", true, 3, -6.5),
                        movement("MLB-BLENDERS", "2026-05", false, 9, -20.0)
                ),
                List.of(
                        movement("MLB-AIR_FRYERS", "2026-07", true, 3, -6.5),
                        movement("MLB-FANS", "2026-07", true, 4, -3.0)
                ),
                List.of(
                        rising("MLB1", "Micro-ondas Electrolux 20L", "electrolux", "electrolux:me23s", "MLB-MICROWAVES",
                                1, 2, 91.2, 579.9, 8),
                        rising("MLB2", "Air fryer Philco", "philco", "philco:pfr", "MLB-AIR_FRYERS",
                                4, 1, 80.0, 299.0, 2)
                ),
                List.of(
                        brand("mondial", "Mondial", 3L, 5L, 2),
                        brand("philco", "Philco", 1L, 2L, 4),
                        brand("arno", "Arno", 0L, 3L, 6)
                ),
                List.of(
                        product("MLB3", "Micro-ondas LG 30L", "lg", "MLB-MICROWAVES", 400.0, 600.0, 12, false),
                        product("MLB4", "Air fryer Mondial", "mondial", "MLB-AIR_FRYERS", 300.0, 330.0, 5, false),
                        product("MLB5", "Ventilador Arno", "arno", "MLB-FANS", 100.0, 160.0, 4, true)
                ),
                List.of(category("MLB-FANS", "Ventilador", 5L, 3L, 2L))
        );
    }

    public static RepairCatalogItem repair(String brand, String model, long volume, String domainId) {
        String slug = brand.toLowerCase();
        return new RepairCatalogItem(brand, model, "TIPO", volume, slug, slug + ":" + model.toLowerCase(), domainId);
    }

    public static MarketCategorySummary category(String domainId, String name, Long products, Long rising, Long high) {
        return new MarketCategorySummary(domainId, BUCKET, name, products, 3L, 70.0, 90.0, 500.0, 300.0, 800.0,
                6.0, 1.0, 2.0, rising, 0L, high);
    }

    public static MarketPriceMovement movement(String domainId, String previous, boolean consecutive, int paired,
                                               Double delta) {
        return new MarketPriceMovement(domainId, BUCKET, previous, consecutive, paired, paired + 2, paired + 1,
                599.0, 540.0, delta);
    }

    public static MarketRisingProduct rising(String id, String title, String brandSlug, String productKey,
                                             String domainId, int rank, int rankDelta, double score,
                                             double priceMedian, int validPrices) {
        return new MarketRisingProduct(BUCKET, "PRODUCT", id, title, brandSlug, brandSlug, "M", productKey,
                domainId, "MLB1", "Categoria ML", rank, "TOP10", rankDelta, "CRESCIMENTO", score, "PRIORIDADE_ALTA",
                "HIGH", priceMedian, priceMedian - 50, priceMedian + 50, 2.0, 1.0, null, validPrices + 1, validPrices,
                false, 3, null, null, null, null, "https://ml/" + id);
    }

    public static MarketBrandSummary brand(String slug, String name, Long rising, Long products, int bestRank) {
        return new MarketBrandSummary(slug, BUCKET, name, products, 2L, 75.0, bestRank, 300.0, 100.0, 900.0,
                1.0, 2.0, rising);
    }

    public static MarketProductCurrent product(String id, String title, String brandSlug, String domainId,
                                               double min, double max, int validPrices, boolean complete) {
        return new MarketProductCurrent(BUCKET, "ML", "PRODUCT", id, title, title, 5, "TOP10", 0, "ESTAVEL",
                60.0, "PRIORIDADE_NORMAL", "HIGH", domainId, "MLB1", "Categoria ML", brandSlug, brandSlug, "M",
                brandSlug + ":m", "127V", 1000, null, null, null, min, min, (min + max) / 2, max, 1.0, 0.0,
                validPrices, validPrices, validPrices, complete, 2, 0.5, "catalog", "https://ml/" + id);
    }
}
