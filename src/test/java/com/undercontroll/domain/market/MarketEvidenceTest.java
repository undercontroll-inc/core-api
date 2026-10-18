package com.undercontroll.domain.market;

import com.undercontroll.domain.model.market.MarketPriceMovement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketEvidenceTest {

    @Test
    @DisplayName("a product median needs at least three valid prices")
    void reliableMedian() {
        assertFalse(MarketEvidence.reliableMedian(null));
        assertFalse(MarketEvidence.reliableMedian(2));
        assertTrue(MarketEvidence.reliableMedian(3));
    }

    @Test
    @DisplayName("a price index needs a consecutive previous month, a delta and enough paired products")
    void reliableMovement() {
        assertTrue(MarketEvidence.reliableMovement(movement("2026-07", true, 3, 5.0)));
        assertFalse(MarketEvidence.reliableMovement(null));
        assertFalse(MarketEvidence.reliableMovement(movement("2026-05", false, 9, 5.0)));
        assertFalse(MarketEvidence.reliableMovement(movement(null, true, 9, 5.0)));
        assertFalse(MarketEvidence.reliableMovement(movement("2026-07", true, 2, 5.0)));
        assertFalse(MarketEvidence.reliableMovement(movement("2026-07", true, null, 5.0)));
        assertFalse(MarketEvidence.reliableMovement(movement("2026-07", true, 9, null)));
    }

    @Test
    @DisplayName("the comparison month is the latest previous month among reliable movements")
    void comparisonBucket() {
        assertEquals("2026-07", MarketEvidence.comparisonBucket(List.of(
                movement("2026-06", false, 9, 1.0),
                movement("2026-07", true, 4, 1.0),
                movement("2026-06", true, 4, 1.0))));
        assertNull(MarketEvidence.comparisonBucket(List.of(movement("2026-07", true, 1, 1.0))));
        assertNull(MarketEvidence.comparisonBucket(null));
    }

    private static MarketPriceMovement movement(String previous, Boolean consecutive, Integer paired, Double delta) {
        return new MarketPriceMovement("MLB-X", "2026-08", previous, consecutive, paired, 5, 5, 100.0, 90.0, delta);
    }
}
