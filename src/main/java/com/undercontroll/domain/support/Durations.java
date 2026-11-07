package com.undercontroll.domain.support;

import java.util.concurrent.TimeUnit;

public final class Durations {

    private Durations() {
    }

    public static long millisSince(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }
}
