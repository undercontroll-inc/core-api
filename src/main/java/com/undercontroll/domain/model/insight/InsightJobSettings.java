package com.undercontroll.domain.model.insight;

import java.time.Duration;

public record InsightJobSettings(
        Duration deadline,
        Duration leaseDuration,
        int maxAttempts,
        Duration rescheduleDelay
) {
    public InsightJobSettings {
        if (deadline == null || deadline.isNegative() || deadline.isZero()) {
            throw new IllegalStateException("undercontroll.ai.insights.job-deadline must be positive");
        }
        if (leaseDuration == null || leaseDuration.compareTo(deadline) <= 0) {
            throw new IllegalStateException("undercontroll.ai.insights.lease-duration must be longer than job-deadline");
        }
        if (maxAttempts < 1) {
            throw new IllegalStateException("undercontroll.ai.insights.max-job-attempts must be at least 1");
        }
        if (rescheduleDelay == null || rescheduleDelay.isNegative()) {
            throw new IllegalStateException("undercontroll.ai.insights.reschedule-delay must not be negative");
        }
    }
}
