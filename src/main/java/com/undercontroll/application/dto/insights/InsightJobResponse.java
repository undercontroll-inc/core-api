package com.undercontroll.application.dto.insights;

import com.undercontroll.domain.model.insight.InsightJob;

public record InsightJobResponse(
        Long jobId,
        long snapshotId,
        String bucketKey,
        String status,
        int attemptCount,
        String nextRunAt,
        String lastError
) {

    public static InsightJobResponse from(InsightJob job) {
        return new InsightJobResponse(
                job.id(),
                job.snapshotId(),
                job.bucketKey(),
                job.status().name(),
                job.attemptCount(),
                job.nextRunAt() == null ? null : job.nextRunAt().toString(),
                job.lastError()
        );
    }
}
