package com.undercontroll.domain.usecase.insights.impl;

import com.undercontroll.domain.exception.InsightsDisabledException;
import com.undercontroll.domain.exception.MarketSnapshotAwaitingException;
import com.undercontroll.domain.model.insight.InsightJob;
import com.undercontroll.domain.usecase.insights.EnqueueInsightJobPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestInsightJobImplTest {

    private final EnqueueInsightJobPort enqueue = mock(EnqueueInsightJobPort.class);
    private final RequestInsightJobImpl useCase = new RequestInsightJobImpl(enqueue);

    @Test
    @DisplayName("an admin request requeues failed jobs and returns the job")
    void returnsJob() {
        InsightJob job = mock(InsightJob.class);
        when(enqueue.execute(true)).thenReturn(new EnqueueInsightJobPort.Result(EnqueueInsightJobPort.Result.Status.ENQUEUED, job));

        assertSame(job, useCase.execute());
    }

    @Test
    @DisplayName("no published snapshot and a disabled provider are explicit errors")
    void errors() {
        when(enqueue.execute(true)).thenReturn(EnqueueInsightJobPort.Result.awaitingData());
        assertThrows(MarketSnapshotAwaitingException.class, useCase::execute);

        when(enqueue.execute(true)).thenReturn(EnqueueInsightJobPort.Result.disabled());
        assertThrows(InsightsDisabledException.class, useCase::execute);
    }
}
