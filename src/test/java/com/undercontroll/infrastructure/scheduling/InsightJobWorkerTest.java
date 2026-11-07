package com.undercontroll.infrastructure.scheduling;

import com.undercontroll.domain.usecase.insights.ProcessInsightJobsPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InsightJobWorkerTest {

    @Test
    @DisplayName("processes due jobs and keeps the schedule alive when processing fails")
    void works() {
        ProcessInsightJobsPort process = mock(ProcessInsightJobsPort.class);
        when(process.execute()).thenReturn(1, 0).thenThrow(new IllegalStateException("db down"));
        InsightJobWorker worker = new InsightJobWorker(process);

        worker.work();
        worker.work();
        assertDoesNotThrow(worker::work);

        verify(process, times(3)).execute();
    }
}
