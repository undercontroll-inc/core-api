package com.undercontroll.domain.usecase.insights.impl;

import com.undercontroll.domain.gateway.InsightJobGateway;
import com.undercontroll.domain.model.insight.InsightJob;
import com.undercontroll.domain.model.insight.InsightJobSettings;
import com.undercontroll.domain.usecase.insights.ProcessInsightJobsPort;
import com.undercontroll.domain.usecase.insights.RunInsightJobPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class ProcessInsightJobsImpl implements ProcessInsightJobsPort {

    static final int BATCH_SIZE = 5;

    private final InsightJobGateway insightJobGateway;
    private final RunInsightJobPort runInsightJobPort;
    private final InsightJobSettings jobSettings;
    private final Clock clock;
    private final String owner = "insights-" + UUID.randomUUID();

    public ProcessInsightJobsImpl(
            InsightJobGateway insightJobGateway,
            RunInsightJobPort runInsightJobPort,
            InsightJobSettings jobSettings,
            Clock clock
    ) {
        this.insightJobGateway = insightJobGateway;
        this.runInsightJobPort = runInsightJobPort;
        this.jobSettings = jobSettings;
        this.clock = clock;
    }

    @Override
    public int execute() {
        int processed = 0;
        for (Long jobId : insightJobGateway.findRunnable(clock.instant(), BATCH_SIZE)) {
            Instant now = clock.instant();
            Optional<InsightJob> leased = insightJobGateway.tryAcquire(jobId, owner, now, now.plus(jobSettings.leaseDuration()));
            if (leased.isEmpty()) {
                continue;
            }
            runInsightJobPort.execute(leased.get(), owner);
            processed++;
        }
        return processed;
    }

    String owner() {
        return owner;
    }
}
