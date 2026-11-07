package com.undercontroll.domain.model.market;

import java.time.Instant;

public record PublishedMarketSnapshot(
        long snapshotId,
        String runId,
        String bucketKey,
        String status,
        Instant publishedAt
) {

    public static final String PARTIAL = "PARTIAL";

    public boolean partial() {
        return PARTIAL.equals(status);
    }
}
