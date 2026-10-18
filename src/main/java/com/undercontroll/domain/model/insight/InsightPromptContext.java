package com.undercontroll.domain.model.insight;

import com.undercontroll.domain.model.market.MatchCoverage;

import java.util.List;
import java.util.Optional;

public record InsightPromptContext(
        String bucketKey,
        String comparisonBucketKey,
        MatchCoverage coverage,
        List<InsightCandidate> candidates
) {
    public InsightPromptContext {
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }

    public Optional<InsightCandidate> candidate(String id) {
        return candidates.stream().filter(candidate -> candidate.id().equals(id)).findFirst();
    }
}
