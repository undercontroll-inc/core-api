package com.undercontroll.domain.model.insight;

import com.undercontroll.domain.enums.InsightAttemptStatus;

import java.time.Instant;

public record InsightAttempt(
        Long id,
        long jobId,
        int attemptNumber,
        InsightAttemptStatus status,
        String draft,
        String validationErrors,
        String errorKind,
        String errorMessage,
        int llmCalls,
        Instant startedAt,
        Instant finishedAt
) {

    public static InsightAttempt start(long jobId, int attemptNumber, Instant now) {
        return new InsightAttempt(null, jobId, attemptNumber, InsightAttemptStatus.GENERATING,
                null, null, null, null, 0, now, null);
    }
}
