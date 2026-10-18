package com.undercontroll.domain.model.insight;

import java.time.Duration;

public record LlmFailure(Kind kind, Integer httpStatus, Duration retryAfter) {

    public enum Kind {
        TRANSIENT,
        RATE_LIMITED,
        TIMEOUT,
        AUTH,
        CONFIG,
        INVALID_OUTPUT,
        UNKNOWN
    }

    public static LlmFailure of(Kind kind) {
        return new LlmFailure(kind, null, null);
    }

    public boolean retryable() {
        return kind == Kind.TRANSIENT || kind == Kind.RATE_LIMITED || kind == Kind.TIMEOUT;
    }
}
