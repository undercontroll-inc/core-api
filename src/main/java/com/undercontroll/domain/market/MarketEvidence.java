package com.undercontroll.domain.market;

import com.undercontroll.domain.model.market.MarketPriceMovement;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;

public final class MarketEvidence {

    public static final int MIN_VALID_PRICES = 3;
    public static final int MIN_PAIRED_PRODUCTS = 3;

    private MarketEvidence() {
    }

    public static boolean reliableMedian(Integer validPriceCount) {
        return validPriceCount != null && validPriceCount >= MIN_VALID_PRICES;
    }

    public static boolean reliableMovement(MarketPriceMovement movement) {
        return movement != null
                && Boolean.TRUE.equals(movement.consecutive())
                && movement.previousBucketKey() != null
                && movement.priceIndexDeltaPct() != null
                && movement.pairedCount() != null
                && movement.pairedCount() >= MIN_PAIRED_PRODUCTS;
    }

    public static String comparisonBucket(Collection<MarketPriceMovement> movements) {
        if (movements == null) {
            return null;
        }
        return movements.stream()
                .filter(MarketEvidence::reliableMovement)
                .map(MarketPriceMovement::previousBucketKey)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }
}
