package com.undercontroll.domain.model.insight;

public record NewInsightJob(
        long snapshotId,
        String bucketKey,
        String promptVersion,
        String modelConfigHash,
        String provider,
        String model
) {
}
