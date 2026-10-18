package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.undercontroll.domain.model.market.MarketBrandSummary;
import com.undercontroll.domain.model.market.MarketCategorySummary;
import com.undercontroll.domain.model.market.MarketPriceMovement;
import com.undercontroll.domain.model.market.MarketProductCurrent;
import com.undercontroll.domain.model.market.MarketRisingProduct;
import com.undercontroll.domain.model.market.MarketSnapshot;
import com.undercontroll.domain.model.market.RepairCatalogLine;
import com.undercontroll.domain.model.market.RepairMixItem;

import java.util.List;

public record InsightGrounding(
        @JsonProperty("cobertura_match") InsightsPayloadJson.CoberturaMatch coberturaMatch,
        @JsonProperty("repair_catalog") List<RepairCatalogLine> repairCatalog,
        @JsonProperty("repair_mix") List<RepairMixItem> repairMix,
        @JsonProperty("market_snapshot") Snapshot marketSnapshot,
        @JsonProperty("price_movements") List<MarketPriceMovement> priceMovements,
        @JsonProperty("stock_opportunities") List<MarketPriceMovement> stockOpportunities,
        @JsonProperty("rising_products") List<MarketRisingProduct> risingProducts,
        @JsonProperty("brand_momentum") List<MarketBrandSummary> brandMomentum,
        @JsonProperty("category_summary") List<MarketCategorySummary> categorySummary,
        @JsonProperty("price_dispersion") List<MarketProductCurrent> priceDispersion,
        @JsonProperty("uncovered_categories") List<MarketCategorySummary> uncoveredCategories
) {
    public record Snapshot(
            long totalProducts,
            long brandsAnalyzed,
            List<MarketSnapshot.BrandLine> topBrands,
            List<MarketSnapshot.CategoryLine> topCategories
    ) {
        static Snapshot from(MarketSnapshot snapshot) {
            if (snapshot == null) {
                return new Snapshot(0L, 0L, List.of(), List.of());
            }
            return new Snapshot(
                    snapshot.totalProducts(),
                    snapshot.brandsAnalyzed(),
                    snapshot.topBrands() == null ? List.of() : snapshot.topBrands(),
                    snapshot.topCategories() == null ? List.of() : snapshot.topCategories()
            );
        }
    }
}
