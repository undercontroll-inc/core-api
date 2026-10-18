package com.undercontroll.domain.model.insight;

import java.time.Instant;

public record MarketInsightPublication(
        Long id,
        long jobId,
        long attemptId,
        long snapshotId,
        String bucketKey,
        String comparisonBucketKey,
        InsightsPayload payload,
        String provider,
        String model,
        String promptVersion,
        Instant publishedAt
) {
}
