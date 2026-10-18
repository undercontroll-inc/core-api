package com.undercontroll.infrastructure.scheduling;

import com.undercontroll.domain.model.insight.InsightGenerationSettings;
import com.undercontroll.domain.usecase.insights.GenerateMonthlyInsightsPort;
import com.undercontroll.domain.usecase.insights.InsightGenerationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MonthlyInsightsSchedulerTest {

    @Test
    @DisplayName("delegates generation to the use case")
    void delegates() {
        GenerateMonthlyInsightsPort port = mock(GenerateMonthlyInsightsPort.class);
        when(port.execute(false)).thenReturn(InsightGenerationResult.success("2026-08"));
        MonthlyInsightsScheduler scheduler = new MonthlyInsightsScheduler(
                port,
                new InsightGenerationSettings("gemini", "gemini-3.6-flash", "6", 7, 90)
        );

        scheduler.generateMonthlyInsights();

        verify(port, times(1)).execute(false);
    }
}
