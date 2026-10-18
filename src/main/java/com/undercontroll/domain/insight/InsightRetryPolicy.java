package com.undercontroll.domain.insight;

import com.undercontroll.domain.model.insight.LlmFailure;

import java.time.Duration;

public record InsightRetryPolicy(int maxRetries, Duration baseDelay, Duration maxDelay) {

    public static final InsightRetryPolicy DEFAULT = new InsightRetryPolicy(2, Duration.ofSeconds(1), Duration.ofSeconds(20));

    public Duration backoff(int retry, LlmFailure failure, double random) {
        long ceiling = Math.min(maxDelay.toMillis(), baseDelay.toMillis() << Math.max(0, retry - 1));
        Duration jitter = Duration.ofMillis((long) (ceiling * Math.min(Math.max(random, 0.0), 1.0)));
        Duration retryAfter = failure.retryAfter();
        return retryAfter != null && retryAfter.compareTo(jitter) > 0 ? retryAfter : jitter;
    }
}
