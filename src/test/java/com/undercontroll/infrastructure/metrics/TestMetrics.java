package com.undercontroll.infrastructure.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.time.Clock;

public final class TestMetrics {

    private TestMetrics() {
    }

    public static FeatureMetrics feature(String feature) {
        return feature(new SimpleMeterRegistry(), feature);
    }

    public static FeatureMetrics feature(SimpleMeterRegistry registry, String feature) {
        return new FeatureMetrics(new AiMetrics(registry, Clock.systemUTC()), feature, "test", "test-model");
    }
}
