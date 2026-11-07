package com.undercontroll.domain.gateway;

import com.undercontroll.domain.model.insight.InsightAttempt;
import com.undercontroll.domain.model.insight.InsightJob;
import com.undercontroll.domain.model.insight.MarketInsightPublication;
import com.undercontroll.domain.model.insight.NewInsightJob;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface InsightJobGateway {

    InsightJob enqueue(NewInsightJob job, Instant now);

    Optional<InsightJob> findById(long jobId);

    Optional<InsightJob> findLatestForSnapshot(long snapshotId);

    List<Long> findRunnable(Instant now, int limit);

    Optional<InsightJob> tryAcquire(long jobId, String owner, Instant now, Instant leaseUntil);

    boolean reschedule(long jobId, String owner, Instant nextRunAt, String error, Instant now);

    boolean fail(long jobId, String owner, String error, Instant now);

    boolean requeue(long jobId, Instant now);

    InsightAttempt saveAttempt(InsightAttempt attempt);

    Optional<InsightAttempt> findLatestAttempt(long jobId);

    MarketInsightPublication publish(MarketInsightPublication publication, String owner, Instant now);
}
