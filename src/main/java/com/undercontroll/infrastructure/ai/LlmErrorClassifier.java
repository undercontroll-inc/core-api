package com.undercontroll.infrastructure.ai;

import com.google.genai.errors.ApiException;
import com.undercontroll.domain.exception.InsightLlmException;
import com.undercontroll.domain.exception.LlmOutputException;
import com.undercontroll.domain.model.insight.LlmFailure;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.TimeoutException;

public final class LlmErrorClassifier {

    private LlmErrorClassifier() {
    }

    public static LlmFailure classify(Throwable error) {
        return classify(error, Clock.systemUTC());
    }

    static LlmFailure classify(Throwable error, Clock clock) {
        Throwable current = error;
        int depth = 0;
        while (current != null && depth++ < 20) {
            LlmFailure failure = direct(current, clock);
            if (failure != null) {
                return failure;
            }
            current = current.getCause();
        }
        return LlmFailure.of(LlmFailure.Kind.UNKNOWN);
    }

    private static LlmFailure direct(Throwable error, Clock clock) {
        if (error instanceof InsightLlmException insight) {
            return insight.failure();
        }
        if (error instanceof LlmOutputException) {
            return LlmFailure.of(LlmFailure.Kind.INVALID_OUTPUT);
        }
        if (error instanceof RestClientResponseException response) {
            return byStatus(response.getStatusCode().value(), retryAfter(response.getResponseHeaders(), clock));
        }
        if (error instanceof WebClientResponseException response) {
            return byStatus(response.getStatusCode().value(), retryAfter(response.getHeaders(), clock));
        }
        if (error instanceof ApiException api) {
            return byStatus(api.code(), null);
        }
        if (error instanceof TimeoutException || error instanceof SocketTimeoutException
                || error instanceof HttpTimeoutException) {
            return LlmFailure.of(LlmFailure.Kind.TIMEOUT);
        }
        if (error instanceof TransientAiException) {
            return LlmFailure.of(LlmFailure.Kind.TRANSIENT);
        }
        if (error instanceof NonTransientAiException) {
            return LlmFailure.of(LlmFailure.Kind.CONFIG);
        }
        if (error instanceof ResourceAccessException && !hasTimeoutCause(error)) {
            return LlmFailure.of(LlmFailure.Kind.TRANSIENT);
        }
        if (error instanceof IOException && !hasTimeoutCause(error)) {
            return LlmFailure.of(LlmFailure.Kind.TRANSIENT);
        }
        return null;
    }

    static LlmFailure byStatus(int status, Duration retryAfter) {
        LlmFailure.Kind kind;
        if (status == 401 || status == 403) {
            kind = LlmFailure.Kind.AUTH;
        } else if (status == 429) {
            kind = LlmFailure.Kind.RATE_LIMITED;
        } else if (status == 408 || status == 504) {
            kind = LlmFailure.Kind.TIMEOUT;
        } else if (status >= 500) {
            kind = LlmFailure.Kind.TRANSIENT;
        } else if (status >= 400) {
            kind = LlmFailure.Kind.CONFIG;
        } else {
            kind = LlmFailure.Kind.UNKNOWN;
        }
        return new LlmFailure(kind, status, retryAfter);
    }

    static Duration retryAfter(HttpHeaders headers, Clock clock) {
        if (headers == null) {
            return null;
        }
        String value = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            long seconds = Long.parseLong(trimmed);
            return seconds < 0 ? null : Duration.ofSeconds(seconds);
        } catch (NumberFormatException notSeconds) {
            try {
                Duration until = Duration.between(clock.instant(),
                        ZonedDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant());
                return until.isNegative() ? Duration.ZERO : until;
            } catch (DateTimeParseException invalid) {
                return null;
            }
        }
    }

    private static boolean hasTimeoutCause(Throwable error) {
        Throwable current = error.getCause();
        while (current != null) {
            if (current instanceof SocketTimeoutException || current instanceof HttpTimeoutException
                    || current instanceof TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
