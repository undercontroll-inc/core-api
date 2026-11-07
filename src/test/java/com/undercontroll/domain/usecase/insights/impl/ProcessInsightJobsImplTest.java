package com.undercontroll.domain.usecase.insights.impl;

import com.undercontroll.domain.enums.InsightJobStatus;
import com.undercontroll.domain.model.insight.InsightJob;
import com.undercontroll.domain.model.insight.InsightJobSettings;
import com.undercontroll.domain.model.insight.NewInsightJob;
import com.undercontroll.domain.usecase.insights.RunInsightJobPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessInsightJobsImplTest {

    private static final Instant NOW = Instant.parse("2026-09-02T04:00:00Z");

    private final InMemoryInsightJobGateway jobs = new InMemoryInsightJobGateway();
    private final RunInsightJobPort run = mock(RunInsightJobPort.class);
    private final ProcessInsightJobsImpl useCase = new ProcessInsightJobsImpl(jobs, run,
            new InsightJobSettings(Duration.ofMinutes(5), Duration.ofMinutes(10), 3, Duration.ofMinutes(5)),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("leases each due job with this worker's id and runs it")
    void processesDueJobs() {
        jobs.enqueue(new NewInsightJob(10L, "2026-08", "7", "a", "gemini", "m"), NOW);
        jobs.enqueue(new NewInsightJob(10L, "2026-08", "7", "b", "gemini", "m"), NOW.plusSeconds(60));
        when(run.execute(any(), any())).thenReturn(RunInsightJobPort.Outcome.PUBLISHED);

        assertEquals(1, useCase.execute());

        ArgumentCaptor<InsightJob> leased = ArgumentCaptor.forClass(InsightJob.class);
        verify(run, times(1)).execute(leased.capture(), eq(useCase.owner()));
        assertEquals(InsightJobStatus.RUNNING, leased.getValue().status());
        assertEquals(1, leased.getValue().attemptCount());
        assertEquals(NOW.plus(Duration.ofMinutes(10)), leased.getValue().leaseUntil());
        assertTrue(useCase.owner().startsWith("insights-"));
    }

    @Test
    @DisplayName("a job another worker leased first is skipped")
    void skipsJobsLeasedElsewhere() {
        InsightJob job = jobs.enqueue(new NewInsightJob(10L, "2026-08", "7", "a", "gemini", "m"), NOW);
        InMemoryInsightJobGateway racing = new InMemoryInsightJobGateway() {
            @Override
            public java.util.List<Long> findRunnable(Instant now, int limit) {
                return java.util.List.of(job.id());
            }

            @Override
            public java.util.Optional<InsightJob> tryAcquire(long jobId, String owner, Instant now, Instant leaseUntil) {
                return java.util.Optional.empty();
            }
        };
        ProcessInsightJobsImpl contended = new ProcessInsightJobsImpl(racing, run,
                new InsightJobSettings(Duration.ofMinutes(5), Duration.ofMinutes(10), 3, Duration.ofMinutes(5)),
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertEquals(0, contended.execute());
        verify(run, times(0)).execute(any(), any());
    }
}
