package com.undercontroll.infrastructure.scheduling;

import com.undercontroll.domain.usecase.insights.GenerateMonthlyInsightsPort;
import com.undercontroll.domain.usecase.insights.InsightGenerationResult;
import com.undercontroll.infrastructure.config.InsightsProperties;
import com.undercontroll.domain.support.Durations;
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

    private final GenerateMonthlyInsightsPort generateMonthlyInsightsPort;
    private final InsightsProperties insightsProperties;

    @Scheduled(cron = "${undercontroll.insights.cron:0 0 4 1 * *}")
    public void generateMonthlyInsights() {
        long started = System.nanoTime();
        MDC.put(MdcKeys.CORRELATION_ID, "insights-" + UUID.randomUUID());
        try {
            log.info("Starting monthly insights job with provider={}", insightsProperties.resolvedProvider());
            InsightGenerationResult result = generateMonthlyInsightsPort.execute(false);
            log.info(
                    "Monthly insights job finished status={} bucketKey={} durationMs={}",
                    result.status(),
                    result.bucketKey(),
                    Durations.millisSince(started)
            );
        } finally {
            MDC.clear();
        }
    }
}
