package com.undercontroll.infrastructure.scheduling;

import com.undercontroll.domain.usecase.insights.EnqueueInsightJobPort;
import com.undercontroll.domain.usecase.insights.ProcessInsightJobsPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MonthlyInsightsSchedulerTest {

    @Test
    @DisplayName("enqueues the job for the published snapshot and then processes due jobs")
    void delegates() {
        EnqueueInsightJobPort enqueue = mock(EnqueueInsightJobPort.class);
        ProcessInsightJobsPort process = mock(ProcessInsightJobsPort.class);
        when(enqueue.execute(false)).thenReturn(EnqueueInsightJobPort.Result.awaitingData());

        new MonthlyInsightsScheduler(enqueue, process).generateMonthlyInsights();

        InOrder order = inOrder(enqueue, process);
        order.verify(enqueue).execute(false);
        order.verify(process).execute();
    }
}
