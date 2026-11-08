package com.undercontroll.infrastructure.storage;

import com.undercontroll.domain.storage.StorageArea;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class PresignedReadCacheTest {

    static final class MutableClock extends Clock {
        Instant now = Instant.parse("2026-11-08T12:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    @DisplayName("reuses a URL until half of its validity is used, then signs a new one")
    void reuseThenResign() {
        MutableClock clock = new MutableClock();
        PresignedReadCache cache = new PresignedReadCache(clock, 100);
        AtomicInteger signed = new AtomicInteger();

        String first = cache.get(StorageArea.ORDER_PHOTOS, "order-photos/1/a.jpg", () -> "url-" + signed.incrementAndGet());
        clock.now = clock.now.plus(Duration.ofMinutes(7));
        String reused = cache.get(StorageArea.ORDER_PHOTOS, "order-photos/1/a.jpg", () -> "url-" + signed.incrementAndGet());
        clock.now = clock.now.plus(Duration.ofMinutes(1));
        String renewed = cache.get(StorageArea.ORDER_PHOTOS, "order-photos/1/a.jpg", () -> "url-" + signed.incrementAndGet());

        assertThat(first).isEqualTo("url-1");
        assertThat(reused).isEqualTo("url-1");
        assertThat(renewed).isEqualTo("url-2");
        assertThat(cache.get(StorageArea.ORDER_PHOTOS, "order-photos/1/b.jpg", () -> "other")).isEqualTo("other");
    }
}
