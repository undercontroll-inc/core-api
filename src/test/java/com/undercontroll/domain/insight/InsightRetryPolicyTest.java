package com.undercontroll.domain.insight;

import com.undercontroll.domain.model.insight.LlmFailure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InsightRetryPolicyTest {

    private final InsightRetryPolicy policy = new InsightRetryPolicy(2, Duration.ofSeconds(1), Duration.ofSeconds(5));
    private final LlmFailure transientFailure = LlmFailure.of(LlmFailure.Kind.TRANSIENT);

    @Test
    @DisplayName("full jitter picks a delay between zero and an exponential ceiling capped at the maximum")
    void fullJitter() {
        assertEquals(Duration.ZERO, policy.backoff(1, transientFailure, 0.0));
        assertEquals(Duration.ofMillis(1000), policy.backoff(1, transientFailure, 1.0));
        assertEquals(Duration.ofMillis(1000), policy.backoff(2, transientFailure, 0.5));
        assertEquals(Duration.ofSeconds(5), policy.backoff(10, transientFailure, 1.0));
        assertEquals(Duration.ofSeconds(1), policy.backoff(1, transientFailure, 7.0));
        assertEquals(Duration.ZERO, policy.backoff(1, transientFailure, -1.0));
    }

    @Test
    @DisplayName("Retry-After is a floor for the delay")
    void retryAfterFloor() {
        LlmFailure limited = new LlmFailure(LlmFailure.Kind.RATE_LIMITED, 429, Duration.ofSeconds(9));

        assertEquals(Duration.ofSeconds(9), policy.backoff(1, limited, 1.0));
        assertEquals(Duration.ofMillis(800), policy.backoff(1,
                new LlmFailure(LlmFailure.Kind.RATE_LIMITED, 429, Duration.ofMillis(100)), 0.8));
    }
}
