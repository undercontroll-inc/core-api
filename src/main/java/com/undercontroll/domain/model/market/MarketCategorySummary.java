package com.undercontroll.domain.model.market;

public record MarketCategorySummary(
        String domainId,
        String bucketKey,
        String categoryName,
        Long productCount,
        Long brandCount,
        Double avgScore,
        Double maxScore,
        Double avgPriceMedian,
        Double priceFloor,
        Double priceCeiling,
        Double avgValidPriceCount,
        Double avgPriceDeltaPct,
        Double avgDiscountPct,
        Long risingCount,
        Long fallingCount,
        Long highConfidenceCount
) {
}
