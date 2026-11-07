package com.undercontroll.domain.model.insight;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InsightJobSettingsTest {

    private static final Duration FIVE = Duration.ofMinutes(5);

    @Test
    @DisplayName("the lease must outlive the job deadline and limits must be positive")
    void validates() {
        assertDoesNotThrow(() -> new InsightJobSettings(FIVE, Duration.ofMinutes(6), 1, Duration.ZERO));
        assertThrows(IllegalStateException.class, () -> new InsightJobSettings(Duration.ZERO, FIVE, 1, FIVE));
        assertThrows(IllegalStateException.class, () -> new InsightJobSettings(null, FIVE, 1, FIVE));
        assertThrows(IllegalStateException.class, () -> new InsightJobSettings(FIVE, FIVE, 1, FIVE));
        assertThrows(IllegalStateException.class, () -> new InsightJobSettings(FIVE, null, 1, FIVE));
        assertThrows(IllegalStateException.class, () -> new InsightJobSettings(FIVE, Duration.ofMinutes(6), 0, FIVE));
        assertThrows(IllegalStateException.class, () -> new InsightJobSettings(FIVE, Duration.ofMinutes(6), 1, null));
        assertThrows(IllegalStateException.class,
                () -> new InsightJobSettings(FIVE, Duration.ofMinutes(6), 1, Duration.ofSeconds(-1)));
    }
}
