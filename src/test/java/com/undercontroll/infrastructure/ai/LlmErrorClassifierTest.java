package com.undercontroll.infrastructure.ai;

import com.google.genai.errors.ClientException;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.errors.ServerException;
import com.undercontroll.domain.exception.InsightLlmException;
import com.undercontroll.domain.exception.LlmOutputException;
import com.undercontroll.domain.model.insight.LlmFailure;
import com.undercontroll.domain.model.insight.LlmFailure.Kind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmErrorClassifierTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-02T04:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("HTTP status decides the kind, whatever the message says")
    void byStatus() {
        assertEquals(Kind.AUTH, kind(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "timeout", null, null, null)));
        assertEquals(Kind.AUTH, kind(HttpClientErrorException.create(HttpStatus.FORBIDDEN, "x", null, null, null)));
        assertEquals(Kind.RATE_LIMITED, kind(HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "x", null, null, null)));
        assertEquals(Kind.CONFIG, kind(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "deadline", null, null, null)));
        assertEquals(Kind.TIMEOUT, kind(HttpServerErrorException.create(HttpStatus.GATEWAY_TIMEOUT, "x", null, null, null)));
        assertEquals(Kind.TRANSIENT, kind(HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "x", null, null, null)));
        assertEquals(Kind.TIMEOUT, LlmErrorClassifier.byStatus(408, null).kind());
        assertEquals(Kind.UNKNOWN, LlmErrorClassifier.byStatus(302, null).kind());
        assertEquals(503, LlmErrorClassifier.classify(
                HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "x", null, null, null)).httpStatus());
    }

    @Test
    @DisplayName("streaming and Gemini SDK errors are classified by their status code, also when wrapped")
    void wrappedErrors() {
        assertEquals(Kind.RATE_LIMITED, kind(WebClientResponseException.create(429, "x", null, null, null)));
        assertEquals(Kind.AUTH, kind(new RuntimeException("Failed to generate content",
                new ClientException(401, "UNAUTHENTICATED", "bad key"))));
        assertEquals(Kind.RATE_LIMITED, kind(new RuntimeException("Failed to generate content",
                new ClientException(429, "RESOURCE_EXHAUSTED", "quota"))));
        assertEquals(Kind.TRANSIENT, kind(new ServerException(500, "INTERNAL", "boom")));
    }

    @Test
    @DisplayName("timeouts, I/O failures and Spring AI retry markers are classified by type")
    void byType() {
        assertEquals(Kind.TIMEOUT, kind(new GenAiIOException("io", new SocketTimeoutException("read timed out"))));
        assertEquals(Kind.TIMEOUT, kind(new ResourceAccessException("x", new SocketTimeoutException("t"))));
        assertEquals(Kind.TIMEOUT, kind(new IllegalStateException(new TimeoutException())));
        assertEquals(Kind.TRANSIENT, kind(new ResourceAccessException("x", new IOException("reset"))));
        assertEquals(Kind.TRANSIENT, kind(new GenAiIOException("io", new IOException("reset"))));
        assertEquals(Kind.TRANSIENT, kind(new TransientAiException("x")));
        assertEquals(Kind.CONFIG, kind(new NonTransientAiException("x")));
        assertEquals(Kind.INVALID_OUTPUT, kind(new LlmOutputException("bad json")));
        assertEquals(Kind.UNKNOWN, kind(new IllegalStateException("request timed out")));
    }

    @Test
    @DisplayName("an already classified failure is kept")
    void keepsClassification() {
        LlmFailure failure = new LlmFailure(Kind.RATE_LIMITED, 429, Duration.ofSeconds(3));

        assertEquals(failure, LlmErrorClassifier.classify(new InsightLlmException(failure, "x", null)));
    }

    @Test
    @DisplayName("Retry-After accepts seconds and HTTP dates and ignores invalid values")
    void retryAfter() {
        assertEquals(Duration.ofSeconds(30), LlmErrorClassifier.retryAfter(headers("30"), CLOCK));
        assertEquals(Duration.ofSeconds(90), LlmErrorClassifier.retryAfter(headers("Wed, 02 Sep 2026 04:01:30 GMT"), CLOCK));
        assertEquals(Duration.ZERO, LlmErrorClassifier.retryAfter(headers("Wed, 02 Sep 2026 03:00:00 GMT"), CLOCK));
        assertNull(LlmErrorClassifier.retryAfter(headers("soon"), CLOCK));
        assertNull(LlmErrorClassifier.retryAfter(headers("-1"), CLOCK));
        assertNull(LlmErrorClassifier.retryAfter(headers(" "), CLOCK));
        assertNull(LlmErrorClassifier.retryAfter(new HttpHeaders(), CLOCK));
        assertNull(LlmErrorClassifier.retryAfter(null, CLOCK));
        LlmFailure limited = LlmErrorClassifier.classify(
                HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "x", headers("4"), null, null), CLOCK);
        assertEquals(Duration.ofSeconds(4), limited.retryAfter());
        assertTrue(limited.retryable());
        assertFalse(LlmFailure.of(Kind.AUTH).retryable());
    }

    private static Kind kind(Throwable error) {
        return LlmErrorClassifier.classify(error, CLOCK).kind();
    }

    private static HttpHeaders headers(String retryAfter) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, retryAfter);
        return headers;
    }
}
