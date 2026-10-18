package com.undercontroll.infrastructure.metrics;

import com.undercontroll.domain.gateway.MetricName;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class MicrometerMetricsAdapterTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final MicrometerMetricsAdapter adapter = new MicrometerMetricsAdapter(registry);

    @Test
    @DisplayName("registers every counter eagerly with the existing meter names and tags")
    void registersExistingMeters() {
        assertThat(registry.find("auth.login.failed").tag("type", "password").counter()).isNotNull();
        assertThat(registry.find("auth.login.failed").tag("type", "google").counter()).isNotNull();
        assertThat(registry.find("auth.login.success").tag("type", "password").counter()).isNotNull();
        assertThat(registry.find("order.created").counter()).isNotNull();
        assertThat(registry.find("inventory.insufficient.stock").counter()).isNotNull();
        assertThat(registry.find("order.processing.time").timer()).isNotNull();
        assertThat(registry.getMeters()).hasSize(MetricName.values().length + 1);
        assertThat(Arrays.stream(MetricName.values()).map(MicrometerMetricsAdapter.MeterSpec::of).distinct())
                .hasSize(MetricName.values().length);
    }

    @Test
    @DisplayName("increments the counter mapped to the metric")
    void increments() {
        adapter.increment(MetricName.GOOGLE_LOGIN_SUCCESS);
        adapter.increment(MetricName.ORDER_CREATED);
        adapter.increment(MetricName.ORDER_CREATED);

        Counter google = registry.find("auth.login.success").tag("type", "google").counter();
        Counter password = registry.find("auth.login.success").tag("type", "password").counter();
        assertThat(google.count()).isEqualTo(1.0);
        assertThat(password.count()).isZero();
        assertThat(registry.find("order.created").counter().count()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("records the order processing time")
    void recordsProcessingTime() {
        adapter.recordOrderProcessingTime(System.currentTimeMillis() - 25);

        assertThat(registry.find("order.processing.time").timer().count()).isEqualTo(1);
    }
}
