package com.undercontroll.domain.usecase.insights.impl;

import com.undercontroll.domain.enums.InsightJobStatus;
import com.undercontroll.domain.gateway.InsightJobGateway;
import com.undercontroll.domain.gateway.InsightsLlmGateway;
import com.undercontroll.domain.gateway.MarketSnapshotGateway;
import com.undercontroll.domain.model.insight.InsightGenerationSettings;
import com.undercontroll.domain.model.insight.InsightJob;
import com.undercontroll.domain.model.insight.NewInsightJob;
import com.undercontroll.domain.model.market.PublishedMarketSnapshot;
import com.undercontroll.domain.usecase.insights.EnqueueInsightJobPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnqueueInsightJobImpl implements EnqueueInsightJobPort {

    private final MarketSnapshotGateway marketSnapshotGateway;
    private final InsightJobGateway insightJobGateway;
    private final ObjectProvider<InsightsLlmGateway> insightsLlmGateway;
    private final InsightGenerationSettings settings;
    private final Clock clock;

    @Override
    public Result execute(boolean requeueFailed) {
        if (insightsLlmGateway.getIfAvailable() == null) {
            return Result.disabled();
        }
        Optional<PublishedMarketSnapshot> published = marketSnapshotGateway.findPublished();
        if (published.isEmpty()) {
            return Result.awaitingData();
        }
        PublishedMarketSnapshot snapshot = published.get();
        Instant now = clock.instant();
        InsightJob job = insightJobGateway.enqueue(new NewInsightJob(
                snapshot.snapshotId(),
                snapshot.bucketKey(),
                settings.promptVersion(),
                settings.modelConfigHash(),
                settings.provider(),
                settings.model()
        ), now);
        if (requeueFailed && job.status() == InsightJobStatus.FAILED && insightJobGateway.requeue(job.id(), now)) {
            log.info("Requeued failed insight job jobId={} snapshotId={}", job.id(), job.snapshotId());
            job = insightJobGateway.findById(job.id()).orElse(job);
        }
        return new Result(Result.Status.ENQUEUED, job);
    }
}
