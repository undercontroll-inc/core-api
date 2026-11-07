package com.undercontroll.domain.usecase.analytics;

import java.util.List;

public interface GetMarketAnalyticsPort {

    Output execute();

    record Output(
            String bucketKey,
            long totalProducts,
            long brandsAnalyzed,
            List<BrandHighlight> topBrands,
            List<CategoryHighlight> topCategories
    ) {
        public static Output empty() {
            return new Output(null, 0L, 0L, List.of(), List.of());
        }
    }

    record BrandHighlight(String name, String slug, long productCount, Integer bestRank, Double avgScore) {
    }

    record CategoryHighlight(String domainId, String name, long productCount, Double avgScore, Long risingCount) {
    }
}
