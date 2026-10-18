package com.undercontroll.infrastructure.metrics;

import java.time.Duration;

public record FeatureMetrics(AiMetrics metrics, String feature, String provider, String model) {

    public void record(String outcome, String finishReason, long startedNanos) {
        metrics.recordCall(feature, provider, model, outcome, finishReason, Duration.ofNanos(System.nanoTime() - startedNanos));
    }

    public void tool(String tool, String outcome) {
        metrics.recordToolCall(feature, tool, outcome);
    }
}
