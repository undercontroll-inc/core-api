package com.undercontroll.domain.model.insight;

import com.undercontroll.domain.model.market.MatchCoverage;

import java.util.List;
import java.util.Optional;

public record InsightGenerationInput(
        String bucketKey,
        String comparisonBucketKey,
        MatchCoverage coverage,
        List<InsightCandidate> candidates,
        List<InsightsPayload.ProdutoEmAlta> risingProducts,
        List<String> limitations
) {
    public InsightGenerationInput {
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
        risingProducts = risingProducts == null ? List.of() : List.copyOf(risingProducts);
        limitations = limitations == null ? List.of() : List.copyOf(limitations);
    }

    public Optional<InsightCandidate> candidate(String id) {
        return candidates.stream().filter(candidate -> candidate.id().equals(id)).findFirst();
    }
}
