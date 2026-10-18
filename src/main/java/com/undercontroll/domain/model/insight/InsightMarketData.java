package com.undercontroll.domain.model.insight;

import com.undercontroll.domain.model.market.MarketBrandSummary;
import com.undercontroll.domain.model.market.MarketCategorySummary;
import com.undercontroll.domain.model.market.MarketPriceMovement;
import com.undercontroll.domain.model.market.MarketProductCurrent;
import com.undercontroll.domain.model.market.MarketRisingProduct;
import com.undercontroll.domain.model.market.RepairCatalogItem;

import java.util.List;

public record InsightMarketData(
        String bucketKey,
        List<RepairCatalogItem> catalog,
        List<MarketCategorySummary> categories,
        List<MarketPriceMovement> priceMovements,
        List<MarketPriceMovement> stockOpportunities,
        List<MarketRisingProduct> risingProducts,
        List<MarketBrandSummary> brandMomentum,
        List<MarketProductCurrent> priceDispersion,
        List<MarketCategorySummary> uncoveredCategories
) {

    public InsightMarketData {
        catalog = copy(catalog);
        categories = copy(categories);
        priceMovements = copy(priceMovements);
        stockOpportunities = copy(stockOpportunities);
        risingProducts = copy(risingProducts);
        brandMomentum = copy(brandMomentum);
        priceDispersion = copy(priceDispersion);
        uncoveredCategories = copy(uncoveredCategories);
    }

    private static <T> List<T> copy(List<T> rows) {
        return rows == null ? List.of() : List.copyOf(rows);
    }
}
