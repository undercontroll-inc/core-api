package com.undercontroll.domain.usecase.insights.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.domain.enums.InsightAttemptStatus;
import com.undercontroll.domain.enums.InsightJobStatus;
import com.undercontroll.domain.exception.InsightLlmException;
import com.undercontroll.domain.exception.MarketDataUnavailableException;
import com.undercontroll.domain.gateway.InsightsLlmGateway;
import com.undercontroll.domain.gateway.MarketSnapshotGateway;
import com.undercontroll.domain.gateway.MarketViewGateway;
import com.undercontroll.domain.insight.InsightContract;
import com.undercontroll.domain.insight.InsightMarketDataLoader;
import com.undercontroll.domain.insight.InsightMarketFixtures;
import com.undercontroll.domain.insight.InsightRetryPolicy;
import com.undercontroll.domain.model.insight.InsightAttempt;
import com.undercontroll.domain.model.insight.InsightDraft;
import com.undercontroll.domain.model.insight.InsightDraftRequest;
import com.undercontroll.domain.model.insight.InsightGenerationSettings;
import com.undercontroll.domain.model.insight.InsightJob;
import com.undercontroll.domain.model.insight.InsightJobSettings;
import com.undercontroll.domain.model.insight.InsightMarketData;
import com.undercontroll.domain.model.insight.LlmFailure;
import com.undercontroll.domain.model.insight.MarketInsightPublication;
import com.undercontroll.domain.model.market.PublishedMarketSnapshot;
import com.undercontroll.domain.usecase.insights.RunInsightJobPort.Outcome;
import com.undercontroll.infrastructure.ai.JacksonInsightDraftCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RunInsightJobImplTest {

    private static final Instant NOW = Instant.parse("2026-09-02T04:00:00Z");
    private static final String OWNER = "worker-1";
    private static final String PRICE_ID = "ALERTA_PRECO:MLB-MICROWAVES";
    private static final InsightDraft VALID = new InsightDraft(List.of(new InsightDraft.Item(PRICE_ID,
            "No {{mes_atual}}, os preços de {{categoria}} subiram {{variacao_preco_pct}} nos mesmos produtos "
                    + "frente ao {{mes_comparacao}}.",
            "Revisar o preço da mão de obra de micro-ondas.")));
    private static final InsightDraft INVALID = new InsightDraft(List.of(new InsightDraft.Item(PRICE_ID,
            "Os preços de {{categoria}} subiram 10,9% no mês, um sinal claro para revisar o orçamento da oficina.",
            "Revisar o preço da mão de obra.")));

    private final MarketSnapshotGateway snapshotGateway = mock(MarketSnapshotGateway.class);
    private final MarketViewGateway marketViewGateway = mock(MarketViewGateway.class);
    private final InsightMarketDataLoader loader = mock(InsightMarketDataLoader.class);
    private final InsightsLlmGateway llm = mock(InsightsLlmGateway.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<InsightsLlmGateway> llmProvider = mock(ObjectProvider.class);
    private final InMemoryInsightJobGateway jobs = new InMemoryInsightJobGateway();
    private final List<Duration> sleeps = new ArrayList<>();
    private RunInsightJobImpl useCase;

    @BeforeEach
    void setUp() {
        when(snapshotGateway.findPublished()).thenReturn(Optional.of(
                new PublishedMarketSnapshot(10L, "run", "2026-08", "SUCCESS", NOW)));
        when(loader.load("2026-08", 90)).thenReturn(InsightMarketFixtures.data());
        when(llmProvider.getIfAvailable()).thenReturn(llm);
        useCase = useCase(new InsightJobSettings(Duration.ofMinutes(5), Duration.ofMinutes(10), 3, Duration.ofMinutes(5)));
    }

    @Test
    @DisplayName("a valid draft is rendered and published with the job, snapshot and model metadata")
    void publishes() {
        when(llm.draft(any())).thenReturn(VALID);

        assertEquals(Outcome.PUBLISHED, useCase.execute(leased(1), OWNER));

        MarketInsightPublication publication = jobs.publications.getFirst();
        assertEquals(10L, publication.snapshotId());
        assertEquals("2026-07", publication.comparisonBucketKey());
        assertEquals("gemini", publication.provider());
        assertTrue(publication.payload().insights().getFirst().texto().startsWith("No mês de agosto de 2026, os preços"));
        assertEquals(InsightJobStatus.SUCCESS, jobs.jobs.get(1L).status());
        InsightAttempt attempt = jobs.attemptsOf(1L).getFirst();
        assertEquals(InsightAttemptStatus.VALID, attempt.status());
        assertEquals(1, attempt.llmCalls());
    }

    @Test
    @DisplayName("a database failure after generation calls the model once and the next run publishes the stored draft")
    void publishFailureNeverRegenerates() {
        when(llm.draft(any())).thenReturn(VALID);
        jobs.publishFailure = new DataAccessResourceFailureException("db down");

        assertEquals(Outcome.RESCHEDULED, useCase.execute(leased(1), OWNER));
        assertEquals(InsightJobStatus.PENDING, jobs.jobs.get(1L).status());
        assertTrue(jobs.jobs.get(1L).lastError().startsWith("PUBLISH: "));
        assertEquals(NOW.plus(Duration.ofMinutes(5)), jobs.jobs.get(1L).nextRunAt());

        jobs.publishFailure = null;
        assertEquals(Outcome.PUBLISHED, useCase.execute(leased(2), OWNER));

        verify(llm, times(1)).draft(any());
        assertEquals(1, jobs.publications.size());
        InsightAttempt reused = jobs.findLatestAttempt(1L).orElseThrow();
        assertEquals(2, reused.attemptNumber());
        assertEquals(0, reused.llmCalls());
    }

    @Test
    @DisplayName("an authentication error is not retried and fails the job")
    void authIsTerminal() {
        when(llm.draft(any())).thenThrow(failure(LlmFailure.Kind.AUTH, 401, null));

        assertEquals(Outcome.FAILED, useCase.execute(leased(1), OWNER));

        verify(llm, times(1)).draft(any());
        assertTrue(sleeps.isEmpty());
        assertTrue(jobs.jobs.get(1L).lastError().startsWith("AUTH: "));
        InsightAttempt attempt = jobs.attemptsOf(1L).getFirst();
        assertEquals(InsightAttemptStatus.FAILED, attempt.status());
        assertEquals("AUTH", attempt.errorKind());
    }

    @Test
    @DisplayName("an invalid draft gets one repair call with the previous draft and its errors")
    void repairsWithFeedback() {
        when(llm.draft(any())).thenReturn(INVALID, VALID);

        assertEquals(Outcome.PUBLISHED, useCase.execute(leased(1), OWNER));

        ArgumentCaptor<InsightDraftRequest> requests = ArgumentCaptor.forClass(InsightDraftRequest.class);
        verify(llm, times(2)).draft(requests.capture());
        InsightDraftRequest repair = requests.getAllValues().get(1);
        assertTrue(repair.repairing());
        assertEquals(INVALID, repair.previousDraft());
        assertTrue(repair.errors().getFirst().contains("writes digits directly"));
        assertEquals(2, jobs.attemptsOf(1L).getFirst().llmCalls());
    }

    @Test
    @DisplayName("a draft still invalid after the repair fails the job without a third call")
    void invalidAfterRepairFails() {
        when(llm.draft(any())).thenReturn(INVALID, INVALID);

        assertEquals(Outcome.FAILED, useCase.execute(leased(1), OWNER));

        verify(llm, times(2)).draft(any());
        assertTrue(jobs.jobs.get(1L).lastError().startsWith("INVALID_OUTPUT: Draft still invalid after one repair"));
        InsightAttempt attempt = jobs.attemptsOf(1L).getFirst();
        assertEquals(InsightAttemptStatus.INVALID, attempt.status());
        assertTrue(attempt.validationErrors().contains("writes digits directly"));
        assertTrue(jobs.publications.isEmpty());
    }

    @Test
    @DisplayName("unparsable output is repaired once with the parse error, and fails if the repair is unusable too")
    void malformedOutput() {
        when(llm.draft(any()))
                .thenThrow(failure(LlmFailure.Kind.INVALID_OUTPUT, null, null))
                .thenThrow(failure(LlmFailure.Kind.INVALID_OUTPUT, null, null));

        assertEquals(Outcome.FAILED, useCase.execute(leased(1), OWNER));

        ArgumentCaptor<InsightDraftRequest> requests = ArgumentCaptor.forClass(InsightDraftRequest.class);
        verify(llm, times(2)).draft(requests.capture());
        assertTrue(requests.getAllValues().get(1).errors().getFirst().startsWith("The previous answer could not be used"));
    }

    @Test
    @DisplayName("transient errors are retried at most twice with jittered backoff, then the job is rescheduled")
    void transientRetries() {
        when(llm.draft(any())).thenThrow(failure(LlmFailure.Kind.TRANSIENT, 503, null));

        assertEquals(Outcome.RESCHEDULED, useCase.execute(leased(1), OWNER));

        verify(llm, times(3)).draft(any());
        assertEquals(List.of(Duration.ofMillis(500), Duration.ofMillis(1000)), sleeps);
        assertEquals(InsightJobStatus.PENDING, jobs.jobs.get(1L).status());
        assertEquals("TRANSIENT", jobs.attemptsOf(1L).getFirst().errorKind());
    }

    @Test
    @DisplayName("Retry-After is honoured when it fits the deadline")
    void retryAfterWithinDeadline() {
        when(llm.draft(any())).thenThrow(failure(LlmFailure.Kind.RATE_LIMITED, 429, Duration.ofSeconds(7))).thenReturn(VALID);

        assertEquals(Outcome.PUBLISHED, useCase.execute(leased(1), OWNER));

        assertEquals(List.of(Duration.ofSeconds(7)), sleeps);
    }

    @Test
    @DisplayName("Retry-After beyond the deadline reschedules the job for after that time without sleeping")
    void retryAfterBeyondDeadline() {
        when(llm.draft(any())).thenThrow(failure(LlmFailure.Kind.RATE_LIMITED, 429, Duration.ofMinutes(30)));

        assertEquals(Outcome.RESCHEDULED, useCase.execute(leased(1), OWNER));

        verify(llm, times(1)).draft(any());
        assertTrue(sleeps.isEmpty());
        assertEquals(NOW.plus(Duration.ofMinutes(30)), jobs.jobs.get(1L).nextRunAt());
    }

    @Test
    @DisplayName("an exhausted job deadline stops before calling the model and reschedules")
    void deadlineExhausted() {
        RunInsightJobImpl expired = useCase(new InsightJobSettings(Duration.ofNanos(1), Duration.ofMinutes(10), 3,
                Duration.ofMinutes(5)));

        assertEquals(Outcome.RESCHEDULED, expired.execute(leased(1), OWNER));

        verify(llm, never()).draft(any());
        assertTrue(jobs.jobs.get(1L).lastError().startsWith("TIMEOUT: "));
    }

    @Test
    @DisplayName("the last allowed attempt fails instead of rescheduling")
    void attemptsExhausted() {
        when(llm.draft(any())).thenThrow(failure(LlmFailure.Kind.TIMEOUT, null, null));

        assertEquals(Outcome.FAILED, useCase.execute(leased(3), OWNER));

        assertTrue(jobs.jobs.get(1L).lastError().endsWith("(attempts exhausted)"));
    }

    @Test
    @DisplayName("a job for a snapshot that is no longer published fails without loading data")
    void superseded() {
        when(snapshotGateway.findPublished()).thenReturn(Optional.of(
                new PublishedMarketSnapshot(11L, "run2", "2026-09", "SUCCESS", NOW)));

        assertEquals(Outcome.FAILED, useCase.execute(leased(1), OWNER));

        verify(loader, never()).load(any(), any(Integer.class));
        assertTrue(jobs.jobs.get(1L).lastError().startsWith("SUPERSEDED: "));
    }

    @Test
    @DisplayName("a disabled provider fails the job with a configuration error")
    void disabled() {
        when(llmProvider.getIfAvailable()).thenReturn(null);

        assertEquals(Outcome.FAILED, useCase.execute(leased(1), OWNER));

        assertTrue(jobs.jobs.get(1L).lastError().startsWith("CONFIG: "));
    }

    @Test
    @DisplayName("unavailable market views reschedule the job without an attempt")
    void marketDataUnavailable() {
        when(loader.load("2026-08", 90)).thenThrow(new MarketDataUnavailableException(new RuntimeException("down")));

        assertEquals(Outcome.RESCHEDULED, useCase.execute(leased(1), OWNER));

        assertTrue(jobs.attemptsOf(1L).isEmpty());
        assertTrue(jobs.jobs.get(1L).lastError().startsWith("MARKET_DATA: "));
    }

    @Test
    @DisplayName("without candidates the payload is published with limitations and no model call")
    void noCandidates() {
        when(loader.load("2026-08", 90)).thenReturn(new InsightMarketData("2026-08", List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of()));
        when(snapshotGateway.findPublished()).thenReturn(Optional.of(
                new PublishedMarketSnapshot(10L, "run", "2026-08", "PARTIAL", NOW)));

        assertEquals(Outcome.PUBLISHED, useCase.execute(leased(1), OWNER));

        verify(llm, never()).draft(any());
        MarketInsightPublication publication = jobs.publications.getFirst();
        assertTrue(publication.payload().insights().isEmpty());
        assertNull(publication.comparisonBucketKey());
        assertEquals(5, publication.payload().limitacoes().size());
    }

    @Test
    @DisplayName("losing the lease before publishing is reported and nothing is stored")
    void leaseLost() {
        when(llm.draft(any())).thenReturn(VALID);
        jobs.leaseLost = true;

        assertEquals(Outcome.LEASE_LOST, useCase.execute(leased(1), OWNER));

        assertTrue(jobs.publications.isEmpty());
    }

    @Test
    @DisplayName("an unexpected error marks the attempt failed and reschedules the job")
    void unexpectedError() {
        when(llm.draft(any())).thenThrow(new IllegalStateException("bug"));

        assertEquals(Outcome.RESCHEDULED, useCase.execute(leased(1), OWNER));

        InsightAttempt attempt = jobs.attemptsOf(1L).getFirst();
        assertEquals(InsightAttemptStatus.FAILED, attempt.status());
        assertEquals("UNKNOWN", attempt.errorKind());
    }

    private InsightJob leased(int attempt) {
        InsightJob current = jobs.jobs.get(1L);
        InsightJob job = new InsightJob(1L, 10L, "2026-08", "7", "hash", "gemini", "m", InsightJobStatus.RUNNING,
                attempt, OWNER, NOW.plusSeconds(600), NOW, current == null ? null : current.lastError(), NOW, NOW, null);
        return jobs.add(job);
    }

    private RunInsightJobImpl useCase(InsightJobSettings jobSettings) {
        return new RunInsightJobImpl(snapshotGateway, marketViewGateway, loader, jobs,
                new JacksonInsightDraftCodec(new ObjectMapper()), llmProvider,
                new InsightGenerationSettings("gemini", "m", "7", 3, 90, "hash"), jobSettings, new InsightContract(3),
                InsightRetryPolicy.DEFAULT, Clock.fixed(NOW, ZoneOffset.UTC), sleeps::add, () -> Long.MIN_VALUE);
    }

    private static InsightLlmException failure(LlmFailure.Kind kind, Integer status, Duration retryAfter) {
        return new InsightLlmException(new LlmFailure(kind, status, retryAfter), kind.name() + ": failed", null);
    }
}
