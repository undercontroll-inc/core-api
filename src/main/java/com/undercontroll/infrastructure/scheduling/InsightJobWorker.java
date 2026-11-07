package com.undercontroll.infrastructure.scheduling;

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
public class InsightJobWorker {

    private final ProcessInsightJobsPort processInsightJobsPort;

    @Scheduled(
            fixedDelayString = "${undercontroll.ai.insights.worker-interval:30s}",
            initialDelayString = "${undercontroll.ai.insights.worker-initial-delay:45s}"
    )
    public void work() {
        MDC.put(MdcKeys.CORRELATION_ID, "insights-job-" + UUID.randomUUID());
        try {
            int processed = processInsightJobsPort.execute();
            if (processed > 0) {
                log.info("Insight worker processed {} job(s)", processed);
            }
        } catch (RuntimeException ex) {
            log.warn("Insight worker failed: {}", ex.toString());
        } finally {
            MDC.clear();
        }
    }
}
