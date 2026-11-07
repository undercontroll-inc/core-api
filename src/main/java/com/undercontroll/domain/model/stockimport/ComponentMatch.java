package com.undercontroll.domain.model.stockimport;

import com.undercontroll.domain.enums.StockImportMatchType;

import java.util.List;

public record ComponentMatch(
        StockImportMatchType type,
        Integer componentId,
        double confidence,
        List<MatchCandidate> candidates
) {

    public static ComponentMatch none(List<MatchCandidate> candidates) {
        return new ComponentMatch(StockImportMatchType.NONE, null, 0.0, candidates);
    }
}
