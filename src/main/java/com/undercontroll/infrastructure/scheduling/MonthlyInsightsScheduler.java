package com.undercontroll.infrastructure.scheduling;

import com.undercontroll.domain.support.Durations;
import com.undercontroll.domain.usecase.insights.EnqueueInsightJobPort;
import com.undercontroll.domain.usecase.insights.ProcessInsightJobsPort;
import com.undercontroll.infrastructure.logging.MdcKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyInsightsScheduler {

    private final EnqueueInsightJobPort enqueueInsightJobPort;
    private final ProcessInsightJobsPort processInsightJobsPort;

    @Scheduled(cron = "${undercontroll.ai.insights.cron:0 0 4 1 * *}", zone = "UTC")
    public void generateMonthlyInsights() {
        long started = System.nanoTime();
        MDC.put(MdcKeys.CORRELATION_ID, "insights-" + UUID.randomUUID());
        try {
            EnqueueInsightJobPort.Result result = enqueueInsightJobPort.execute(false);
            int processed = processInsightJobsPort.execute();
            log.info("Monthly insights job finished status={} processed={} durationMs={}",
                    result.status(), processed, Durations.millisSince(started));
        } finally {
            MDC.clear();
        }
    }
}
