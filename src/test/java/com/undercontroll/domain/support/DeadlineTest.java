package com.undercontroll.domain.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class DeadlineTest {

    @Test
    @DisplayName("remaining time shrinks with the clock, caps each step and never goes negative")
    void remaining() {
        AtomicLong now = new AtomicLong(0);
        Deadline deadline = Deadline.after(Duration.ofSeconds(60), now::get);

        assertThat(deadline.cap(Duration.ofSeconds(30))).isEqualTo(Duration.ofSeconds(30));
        now.set(Duration.ofSeconds(50).toNanos());
        assertThat(deadline.remaining()).isEqualTo(Duration.ofSeconds(10));
        assertThat(deadline.cap(Duration.ofSeconds(30))).isEqualTo(Duration.ofSeconds(10));
        assertThat(deadline.expired()).isFalse();
        now.set(Duration.ofSeconds(61).toNanos());
        assertThat(deadline.remaining()).isZero();
        assertThat(deadline.expired()).isTrue();
        assertThat(Deadline.after(Duration.ofSeconds(1)).expired()).isFalse();
    }
}
