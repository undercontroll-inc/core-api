package com.undercontroll.domain.usecase.insights.impl;

import com.undercontroll.domain.enums.InsightJobStatus;
import com.undercontroll.domain.gateway.InsightsLlmGateway;
import com.undercontroll.domain.gateway.MarketSnapshotGateway;
import com.undercontroll.domain.model.insight.InsightGenerationSettings;
import com.undercontroll.domain.model.insight.InsightJob;
import com.undercontroll.domain.model.market.PublishedMarketSnapshot;
import com.undercontroll.domain.usecase.insights.EnqueueInsightJobPort.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnqueueInsightJobImplTest {

    private static final Instant NOW = Instant.parse("2026-09-02T04:00:00Z");

    private final MarketSnapshotGateway snapshots = mock(MarketSnapshotGateway.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<InsightsLlmGateway> llm = mock(ObjectProvider.class);
    private final InMemoryInsightJobGateway jobs = new InMemoryInsightJobGateway();
    private final EnqueueInsightJobImpl useCase = new EnqueueInsightJobImpl(snapshots, jobs, llm,
            new InsightGenerationSettings("gemini", "gemini-2.5-flash", "7", 5, 90, "hash"),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void setUp() {
        when(llm.getIfAvailable()).thenReturn(mock(InsightsLlmGateway.class));
        when(snapshots.findPublished()).thenReturn(Optional.of(
                new PublishedMarketSnapshot(10L, "run", "2026-08", "SUCCESS", NOW)));
    }

    @Test
    @DisplayName("creates one job per snapshot and configuration")
    void enqueues() {
        Result first = useCase.execute(false);
        Result again = useCase.execute(false);

        assertEquals(Result.Status.ENQUEUED, first.status());
        InsightJob job = first.job();
        assertEquals(10L, job.snapshotId());
        assertEquals("2026-08", job.bucketKey());
        assertEquals("7", job.promptVersion());
        assertEquals("hash", job.modelConfigHash());
        assertEquals("gemini-2.5-flash", job.model());
        assertEquals(job.id(), again.job().id());
        assertEquals(1, jobs.jobs.size());
    }

    @Test
    @DisplayName("a failed job is only requeued when asked")
    void requeuesFailedJobOnRequest() {
        InsightJob job = useCase.execute(false).job();
        jobs.tryAcquire(job.id(), "w", NOW, NOW.plusSeconds(60));
        jobs.fail(job.id(), "w", "AUTH: 401", NOW);

        assertEquals(InsightJobStatus.FAILED, useCase.execute(false).job().status());
        Result requeued = useCase.execute(true);

        assertEquals(InsightJobStatus.PENDING, requeued.job().status());
        assertEquals(0, requeued.job().attemptCount());
    }

    @Test
    @DisplayName("without a published snapshot or a provider nothing is enqueued")
    void skips() {
        when(snapshots.findPublished()).thenReturn(Optional.empty());
        assertEquals(Result.awaitingData(), useCase.execute(true));

        when(llm.getIfAvailable()).thenReturn(null);
        Result disabled = useCase.execute(true);
        assertEquals(Result.Status.DISABLED, disabled.status());
        assertNull(disabled.job());
        assertEquals(0, jobs.jobs.size());
    }
}
