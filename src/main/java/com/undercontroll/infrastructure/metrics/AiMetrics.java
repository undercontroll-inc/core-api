package com.undercontroll.infrastructure.metrics;

import com.undercontroll.domain.gateway.InsightMetricsGateway;
import com.undercontroll.infrastructure.ai.config.AiFeatureSettings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class AiMetrics implements InsightMetricsGateway {

    public static final String CALL = "undercontroll.ai.call";
    public static final String TOOL_CALLS = "undercontroll.ai.tool.calls";
    public static final String JOB = "undercontroll.insights.job";
    public static final String VALIDATION = "undercontroll.insights.draft.validation";
    public static final String SNAPSHOT_AGE = "undercontroll.market.snapshot.age";
    public static final String PUBLICATION_AGE = "undercontroll.insights.publication.age";

    static final Duration[] CALL_SLO = seconds(0.5, 1, 2, 5, 10, 20, 30, 60, 90, 120);
    static final Duration[] JOB_SLO = seconds(1, 5, 10, 30, 60, 120, 180, 300, 600);
    static final String NONE = "none";

    private final MeterRegistry registry;
    private final Clock clock;
    private final AtomicReference<Instant> snapshotPublishedAt = new AtomicReference<>();
    private final AtomicReference<Instant> insightsPublishedAt = new AtomicReference<>();

    public AiMetrics(MeterRegistry registry, Clock clock) {
        this.registry = registry;
        this.clock = clock;
        Gauge.builder(SNAPSHOT_AGE, this, metrics -> metrics.ageSeconds(metrics.snapshotPublishedAt.get()))
                .description("Seconds since the published market snapshot was published by the ETL")
                .baseUnit("seconds")
                .register(registry);
        Gauge.builder(PUBLICATION_AGE, this, metrics -> metrics.ageSeconds(metrics.insightsPublishedAt.get()))
                .description("Seconds since the latest insight publication")
                .baseUnit("seconds")
                .register(registry);
    }

    public FeatureMetrics feature(AiFeatureSettings settings) {
        return new FeatureMetrics(this, settings.feature(), settings.provider().label(), String.valueOf(settings.model()));
    }

    void recordCall(String feature, String provider, String model, String outcome, String finishReason, Duration duration) {
        Timer.builder(CALL)
                .description("AI call duration by feature, provider, model, outcome and finish reason")
                .tag("feature", feature)
                .tag("provider", provider)
                .tag("model", model)
                .tag("outcome", normalize(outcome))
                .tag("finish_reason", normalize(finishReason))
                .serviceLevelObjectives(CALL_SLO)
                .register(registry)
                .record(duration);
    }

    public void recordToolCall(String feature, String tool, String outcome) {
        Counter.builder(TOOL_CALLS)
                .description("Tool calls requested by the model, executed or refused by the budget")
                .tag("feature", feature)
                .tag("tool", normalize(tool))
                .tag("outcome", normalize(outcome))
                .register(registry)
                .increment();
    }

    @Override
    public void recordJob(String outcome, Duration duration) {
        Timer.builder(JOB)
                .description("Insight job run duration by outcome")
                .tag("outcome", normalize(outcome))
                .serviceLevelObjectives(JOB_SLO)
                .register(registry)
                .record(duration);
    }

    @Override
    public void recordDraftValidation(boolean repair, boolean valid) {
        Counter.builder(VALIDATION)
                .description("Insight draft validations by stage and result")
                .tag("stage", repair ? "repair" : "first")
                .tag("result", valid ? "valid" : "invalid")
                .register(registry)
                .increment();
    }

    @Override
    public void snapshotPublished(Instant publishedAt) {
        if (publishedAt != null) {
            snapshotPublishedAt.set(publishedAt);
        }
    }

    @Override
    public void insightsPublished(Instant publishedAt) {
        if (publishedAt != null) {
            insightsPublishedAt.set(publishedAt);
        }
    }

    private double ageSeconds(Instant instant) {
        return instant == null ? Double.NaN : Math.max(0, Duration.between(instant, clock.instant()).toSeconds());
    }

    static String normalize(String value) {
        return value == null || value.isBlank() ? NONE : value.trim().toLowerCase(Locale.ROOT);
    }

    private static Duration[] seconds(double... values) {
        Duration[] durations = new Duration[values.length];
        for (int index = 0; index < values.length; index++) {
            durations[index] = Duration.ofMillis(Math.round(values[index] * 1000));
        }
        return durations;
    }
}
