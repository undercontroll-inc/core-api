package com.undercontroll.infrastructure.metrics;

import com.undercontroll.infrastructure.ai.config.AiFeatureSettings;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.CountAtBucket;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiMetricsTest {

    private static final Instant NOW = Instant.parse("2026-09-02T04:00:00Z");

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final AiMetrics metrics = new AiMetrics(registry, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("AI calls are timed per feature, provider, model, outcome and finish reason with buckets up to 120s")
    void calls() {
        AiFeatureSettings settings = AiFeatureSettings.chat("insights", "gemini", "gemini-2.5-flash",
                Duration.ofSeconds(60), 0.2, 1024, "default");

        metrics.feature(settings).record("success", "STOP", System.nanoTime() - Duration.ofSeconds(42).toNanos());
        metrics.feature(settings).record("RATE_LIMITED", null, System.nanoTime());

        Timer success = registry.find(AiMetrics.CALL)
                .tags("feature", "insights", "provider", "gemini", "model", "gemini-2.5-flash",
                        "outcome", "success", "finish_reason", "stop")
                .timer();
        assertEquals(1, success.count());
        assertEquals(1, registry.find(AiMetrics.CALL).tags("outcome", "rate_limited", "finish_reason", "none").timer().count());
        List<Double> buckets = Arrays.stream(success.takeSnapshot().histogramCounts())
                .map(CountAtBucket::bucket).toList();
        assertEquals(Duration.ofSeconds(120).toNanos(), buckets.getLast(), 1.0);
        assertTrue(buckets.contains((double) Duration.ofSeconds(60).toNanos()));
        assertEquals(Set.of("feature", "provider", "model", "outcome", "finish_reason"),
                success.getId().getTags().stream().map(Tag::getKey).collect(Collectors.toSet()));
    }

    @Test
    @DisplayName("tool calls, job runs and draft validations are counted with bounded tags")
    void counters() {
        metrics.recordToolCall("ana", "search_web", "refused");
        metrics.recordJob("PUBLISHED", Duration.ofSeconds(30));
        metrics.recordDraftValidation(false, false);
        metrics.recordDraftValidation(true, true);

        assertEquals(1, registry.find(AiMetrics.TOOL_CALLS).tags("feature", "ana", "tool", "search_web", "outcome", "refused")
                .counter().count());
        assertEquals(1, registry.find(AiMetrics.JOB).tag("outcome", "published").timer().count());
        assertEquals(1, registry.find(AiMetrics.VALIDATION).tags("stage", "first", "result", "invalid").counter().count());
        assertEquals(1, registry.find(AiMetrics.VALIDATION).tags("stage", "repair", "result", "valid").counter().count());
    }

    @Test
    @DisplayName("snapshot and publication ages are unknown until observed, then measured in seconds")
    void ages() {
        assertTrue(Double.isNaN(registry.find(AiMetrics.SNAPSHOT_AGE).gauge().value()));
        assertTrue(Double.isNaN(registry.find(AiMetrics.PUBLICATION_AGE).gauge().value()));

        metrics.snapshotPublished(NOW.minusSeconds(3600));
        metrics.insightsPublished(NOW.minusSeconds(60));
        metrics.snapshotPublished(null);
        metrics.insightsPublished(null);

        assertEquals(3600.0, registry.find(AiMetrics.SNAPSHOT_AGE).gauge().value());
        assertEquals(60.0, registry.find(AiMetrics.PUBLICATION_AGE).gauge().value());
    }
}
