package com.undercontroll.domain.model.insight;

import com.undercontroll.domain.enums.InsightJobStatus;

import java.time.Instant;

public record InsightJob(
        Long id,
        long snapshotId,
        String bucketKey,
        String promptVersion,
        String modelConfigHash,
        String provider,
        String model,
        InsightJobStatus status,
        int attemptCount,
        String leaseOwner,
        Instant leaseUntil,
        Instant nextRunAt,
        String lastError,
        Instant createdAt,
        Instant updatedAt,
        Instant finishedAt
) {

    public boolean active() {
        return status == InsightJobStatus.PENDING || status == InsightJobStatus.RUNNING;
    }
}
