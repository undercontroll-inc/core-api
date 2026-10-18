package com.undercontroll.infrastructure.ai;

import com.undercontroll.domain.gateway.MarketViewGateway;
import com.undercontroll.domain.model.market.MarketBrandSummary;
import com.undercontroll.domain.model.market.MarketCategorySummary;
import com.undercontroll.domain.model.market.MarketPriceMovement;
import com.undercontroll.domain.model.market.MarketProductCurrent;
import com.undercontroll.domain.model.market.MarketRisingProduct;
import com.undercontroll.domain.model.market.MarketSnapshot;
import com.undercontroll.domain.model.market.RepairCatalogItem;
import com.undercontroll.domain.model.market.RepairCatalogLine;
import com.undercontroll.domain.model.market.RepairMixItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InsightGroundingBuilder {

    static final int REPAIR_CATALOG_LIMIT = 25;
    static final int SNAPSHOT_TOP_N = 5;
    static final int PRICE_MOVEMENTS_LIMIT = 15;
    static final int STOCK_LIMIT = 10;
    static final int RISING_LIMIT = 10;
    static final int BRAND_LIMIT = 10;
    static final int CATEGORY_LIMIT = 15;
    static final int DISPERSION_LIMIT = 10;
    static final int UNCOVERED_LIMIT = 10;

    private final MarketViewGateway marketViewGateway;

    public InsightGrounding build(InsightGenerationContext.State state) {
        return new InsightGrounding(
                matchCoverage(state),
                repairCatalog(state),
                repairMix(state),
                InsightGrounding.Snapshot.from(marketSnapshot(state)),
                priceMovements(state),
                stockOpportunities(state),
                risingProducts(state),
                brandMomentum(state),
                categorySummary(state),
                priceDispersion(state),
                uncoveredCategories(state)
        );
    }

    InsightsPayloadJson.CoberturaMatch matchCoverage(InsightGenerationContext.State state) {
        InsightsPayloadJson.CoberturaMatch coverage = InsightsPayloadJson.CoberturaMatch.from(state.coverage());
        state.evidence().ingest(coverage);
        return coverage;
    }

    List<RepairCatalogLine> repairCatalog(InsightGenerationContext.State state) {
        List<RepairCatalogLine> rows = cap(state.catalog().stream()
                .sorted(Comparator.comparingLong(RepairCatalogItem::volume).reversed())
                .map(item -> new RepairCatalogLine(
                        item.brand(),
                        item.model(),
                        item.type(),
                        item.domainId(),
                        item.volume()
                ))
                .toList(), REPAIR_CATALOG_LIMIT);
        state.evidence().ingest(rows);
        return rows;
    }

    List<RepairMixItem> repairMix(InsightGenerationContext.State state) {
        List<RepairCatalogItem> catalog = state.catalog();
        long total = catalog.stream().mapToLong(RepairCatalogItem::volume).sum();
        record MixKey(String domainId, String type) {
        }
        Map<MixKey, Long> volumes = catalog.stream().collect(Collectors.groupingBy(
                item -> new MixKey(item.domainId(), item.type()),
                Collectors.summingLong(RepairCatalogItem::volume)
        ));
        List<RepairMixItem> rows = volumes.entrySet().stream()
                .sorted(Map.Entry.<MixKey, Long>comparingByValue().reversed())
                .map(entry -> new RepairMixItem(
                        entry.getKey().domainId(),
                        entry.getKey().type(),
                        entry.getValue(),
                        total == 0 ? 0.0 : Math.round(entry.getValue() * 1000.0 / total) / 10.0
                ))
                .toList();
        state.evidence().ingest(rows);
        return rows;
    }

    MarketSnapshot marketSnapshot(InsightGenerationContext.State state) {
        String bucketKey = state.bucketKey();
        List<MarketSnapshot.BrandLine> brands = marketViewGateway.findTopBrands(bucketKey, SNAPSHOT_TOP_N).stream()
                .map(row -> new MarketSnapshot.BrandLine(
                        row.brandName(),
                        row.brandSlug(),
                        row.productCount() == null ? 0L : row.productCount(),
                        row.bestRank(),
                        row.avgScore()
                ))
                .toList();
        List<MarketSnapshot.CategoryLine> categories = marketViewGateway.findTopCategories(bucketKey, SNAPSHOT_TOP_N).stream()
                .map(row -> new MarketSnapshot.CategoryLine(
                        row.domainId(),
                        row.categoryName(),
                        row.productCount() == null ? 0L : row.productCount(),
                        row.avgScore(),
                        row.risingCount()
                ))
                .toList();
        MarketSnapshot snapshot = new MarketSnapshot(
                bucketKey,
                marketViewGateway.countCurrentProducts(),
                marketViewGateway.countDistinctBrands(),
                brands,
                categories
        );
        state.evidence().ingest(snapshot);
        return snapshot;
    }

    List<MarketPriceMovement> priceMovements(InsightGenerationContext.State state) {
        List<MarketPriceMovement> rows = cap(
                marketViewGateway.findPriceMovements(state.bucketKey()), PRICE_MOVEMENTS_LIMIT);
        state.evidence().ingest(rows);
        return rows;
    }

    List<MarketPriceMovement> stockOpportunities(InsightGenerationContext.State state) {
        List<MarketPriceMovement> rows = cap(
                marketViewGateway.findStockOpportunities(state.bucketKey()), STOCK_LIMIT);
        state.evidence().ingest(rows);
        return rows;
    }

    List<MarketRisingProduct> risingProducts(InsightGenerationContext.State state) {
        List<MarketRisingProduct> rows = cap(
                marketViewGateway.findRisingProductsHighConfidence(), RISING_LIMIT);
        state.evidence().ingest(rows);
        return rows;
    }

    List<MarketBrandSummary> brandMomentum(InsightGenerationContext.State state) {
        List<MarketBrandSummary> rows = cap(
                marketViewGateway.findBrandMomentum(state.bucketKey()), BRAND_LIMIT);
        state.evidence().ingest(rows);
        return rows;
    }

    List<MarketCategorySummary> categorySummary(InsightGenerationContext.State state) {
        List<MarketCategorySummary> rows = cap(state.categories(), CATEGORY_LIMIT);
        state.evidence().ingest(rows);
        return rows;
    }

    List<MarketProductCurrent> priceDispersion(InsightGenerationContext.State state) {
        List<MarketProductCurrent> rows = cap(marketViewGateway.findPriceDispersion(), DISPERSION_LIMIT);
        state.evidence().ingest(rows);
        return rows;
    }

    List<MarketCategorySummary> uncoveredCategories(InsightGenerationContext.State state) {
        List<MarketCategorySummary> rows = cap(
                marketViewGateway.findUncoveredCategories(state.bucketKey(), state.clientDomainIds()),
                UNCOVERED_LIMIT);
        state.evidence().ingest(rows);
        return rows;
    }

    static <T> List<T> cap(List<T> rows, int limit) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.size() <= limit ? rows : List.copyOf(rows.subList(0, limit));
    }
}
