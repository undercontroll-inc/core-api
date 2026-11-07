package com.undercontroll.domain.insight;

import com.undercontroll.domain.gateway.MarketViewGateway;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.market.RepairCatalogFactory;
import com.undercontroll.domain.market.RepairCatalogMatcher;
import com.undercontroll.domain.model.insight.InsightMarketData;
import com.undercontroll.domain.model.market.MarketCategorySummary;
import com.undercontroll.domain.model.market.RepairCatalogItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class InsightMarketDataLoader {

    private final MarketViewGateway marketViewGateway;
    private final OrderGateway orderGateway;
    private final Clock clock;

    public InsightMarketData load(String bucketKey, int repairCatalogDays) {
        List<MarketCategorySummary> categories = marketViewGateway.findCategorySummary(bucketKey);
        List<RepairCatalogItem> catalog = RepairCatalogFactory.fromRows(
                orderGateway.getRepairCatalog(LocalDate.now(clock).minusDays(repairCatalogDays)),
                categories
        );
        return new InsightMarketData(
                bucketKey,
                catalog,
                categories,
                marketViewGateway.findPriceMovements(bucketKey),
                marketViewGateway.findStockOpportunities(bucketKey),
                marketViewGateway.findRisingProductsHighConfidence(),
                marketViewGateway.findBrandMomentum(bucketKey),
                marketViewGateway.findPriceDispersion(),
                marketViewGateway.findUncoveredCategories(bucketKey, RepairCatalogMatcher.clientDomainIds(catalog))
        );
    }
}
