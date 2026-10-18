package com.undercontroll.domain.support;

import java.time.Duration;
import java.util.function.LongSupplier;

public final class Deadline {

    private final long expiresAtNanos;
    private final LongSupplier nanoClock;

    private Deadline(long expiresAtNanos, LongSupplier nanoClock) {
        this.expiresAtNanos = expiresAtNanos;
        this.nanoClock = nanoClock;
    }

    public static Deadline after(Duration budget) {
        return after(budget, System::nanoTime);
    }

    public static Deadline after(Duration budget, LongSupplier nanoClock) {
        return new Deadline(nanoClock.getAsLong() + budget.toNanos(), nanoClock);
    }

    public Duration remaining() {
        long left = expiresAtNanos - nanoClock.getAsLong();
        return left <= 0 ? Duration.ZERO : Duration.ofNanos(left);
    }

    public boolean expired() {
        return remaining().isZero();
    }

    public Duration cap(Duration step) {
        Duration left = remaining();
        return step.compareTo(left) < 0 ? step : left;
    }
}
