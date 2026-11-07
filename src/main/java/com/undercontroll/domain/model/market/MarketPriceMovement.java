package com.undercontroll.domain.model.market;

public record MarketPriceMovement(
        String domainId,
        String bucketKey,
        String previousBucketKey,
        Boolean consecutive,
        Integer pairedCount,
        Integer currentProductCount,
        Integer previousProductCount,
        Double currentCohortMedian,
        Double previousCohortMedian,
        Double priceIndexDeltaPct
) {
}
