package com.undercontroll.domain.market;

import com.undercontroll.domain.model.market.MarketCategorySummary;
import com.undercontroll.domain.model.market.RepairCatalogItem;
import com.undercontroll.domain.model.market.RepairCatalogRow;

import java.util.List;

public final class RepairCatalogFactory {

    private RepairCatalogFactory() {
    }

    public static List<RepairCatalogItem> fromRows(List<RepairCatalogRow> rows, List<MarketCategorySummary> categories) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .map(row -> new RepairCatalogItem(
                        row.brand(),
                        row.model(),
                        row.type(),
                        row.volume(),
                        MarketSlug.slugify(row.brand()),
                        MarketSlug.productKey(row.brand(), row.model()),
                        ApplianceTypeDomainMap.resolve(row.type(), categories)
                ))
                .toList();
    }
}
