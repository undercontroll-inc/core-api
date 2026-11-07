package com.undercontroll.domain.usecase.insights.impl;

import com.undercontroll.domain.enums.InsightJobStatus;
import com.undercontroll.domain.exception.InsightLeaseLostException;
import com.undercontroll.domain.gateway.InsightJobGateway;
import com.undercontroll.domain.model.insight.InsightAttempt;
import com.undercontroll.domain.model.insight.InsightJob;
import com.undercontroll.domain.model.insight.MarketInsightPublication;
import com.undercontroll.domain.model.insight.NewInsightJob;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class InMemoryInsightJobGateway implements InsightJobGateway {

    final Map<Long, InsightJob> jobs = new LinkedHashMap<>();
    final Map<Long, InsightAttempt> attempts = new LinkedHashMap<>();
    final List<MarketInsightPublication> publications = new ArrayList<>();
    RuntimeException publishFailure;
    boolean leaseLost;
    private long nextId = 1;

    InsightJob add(InsightJob job) {
        jobs.put(job.id(), job);
        return job;
    }

    @Override
    public InsightJob enqueue(NewInsightJob job, Instant now) {
        return jobs.values().stream()
                .filter(existing -> existing.snapshotId() == job.snapshotId()
                        && existing.promptVersion().equals(job.promptVersion())
                        && existing.modelConfigHash().equals(job.modelConfigHash()))
                .findFirst()
                .orElseGet(() -> add(new InsightJob(nextId++, job.snapshotId(), job.bucketKey(), job.promptVersion(),
                        job.modelConfigHash(), job.provider(), job.model(), InsightJobStatus.PENDING, 0, null, null,
                        now, null, now, now, null)));
    }

    @Override
    public Optional<InsightJob> findById(long jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    @Override
    public Optional<InsightJob> findLatestForSnapshot(long snapshotId) {
        return jobs.values().stream().filter(job -> job.snapshotId() == snapshotId).reduce((first, second) -> second);
    }

    @Override
    public List<Long> findRunnable(Instant now, int limit) {
        return jobs.values().stream()
                .filter(job -> job.status() == InsightJobStatus.PENDING && !job.nextRunAt().isAfter(now))
                .map(InsightJob::id)
                .limit(limit)
                .toList();
    }

    @Override
    public Optional<InsightJob> tryAcquire(long jobId, String owner, Instant now, Instant leaseUntil) {
        InsightJob job = jobs.get(jobId);
        if (job == null || job.status() != InsightJobStatus.PENDING) {
            return Optional.empty();
        }
        return Optional.of(add(with(job, InsightJobStatus.RUNNING, job.attemptCount() + 1, owner, leaseUntil,
                job.nextRunAt(), job.lastError(), null)));
    }

    @Override
    public boolean reschedule(long jobId, String owner, Instant nextRunAt, String error, Instant now) {
        InsightJob job = owned(jobId, owner);
        if (job == null) {
            return false;
        }
        add(with(job, InsightJobStatus.PENDING, job.attemptCount(), null, null, nextRunAt, error, null));
        return true;
    }

    @Override
    public boolean fail(long jobId, String owner, String error, Instant now) {
        InsightJob job = owned(jobId, owner);
        if (job == null) {
            return false;
        }
        add(with(job, InsightJobStatus.FAILED, job.attemptCount(), null, null, job.nextRunAt(), error, now));
        return true;
    }

    @Override
    public boolean requeue(long jobId, Instant now) {
        InsightJob job = jobs.get(jobId);
        if (job == null || job.status() != InsightJobStatus.FAILED) {
            return false;
        }
        add(with(job, InsightJobStatus.PENDING, 0, null, null, now, null, null));
        return true;
    }

    @Override
    public InsightAttempt saveAttempt(InsightAttempt attempt) {
        InsightAttempt saved = attempt.id() == null
                ? new InsightAttempt(nextId++, attempt.jobId(), attempt.attemptNumber(), attempt.status(), attempt.draft(),
                attempt.validationErrors(), attempt.errorKind(), attempt.errorMessage(), attempt.llmCalls(),
                attempt.startedAt(), attempt.finishedAt())
                : attempt;
        attempts.put(saved.id(), saved);
        return saved;
    }

    @Override
    public Optional<InsightAttempt> findLatestAttempt(long jobId) {
        return attempts.values().stream()
                .filter(attempt -> attempt.jobId() == jobId)
                .max(Comparator.comparingInt(InsightAttempt::attemptNumber).thenComparing(InsightAttempt::id));
    }

    @Override
    public MarketInsightPublication publish(MarketInsightPublication publication, String owner, Instant now) {
        if (publishFailure != null) {
            throw publishFailure;
        }
        if (leaseLost || owned(publication.jobId(), owner) == null) {
            throw new InsightLeaseLostException(publication.jobId());
        }
        InsightJob job = jobs.get(publication.jobId());
        add(with(job, InsightJobStatus.SUCCESS, job.attemptCount(), null, null, job.nextRunAt(), null, now));
        publications.add(publication);
        return publication;
    }

    List<InsightAttempt> attemptsOf(long jobId) {
        return attempts.values().stream().filter(attempt -> attempt.jobId() == jobId).toList();
    }

    private InsightJob owned(long jobId, String owner) {
        InsightJob job = jobs.get(jobId);
        return job != null && job.status() == InsightJobStatus.RUNNING && owner.equals(job.leaseOwner()) ? job : null;
    }

    private static InsightJob with(InsightJob job, InsightJobStatus status, int attempts, String owner,
                                   Instant leaseUntil, Instant nextRunAt, String error, Instant finishedAt) {
        return new InsightJob(job.id(), job.snapshotId(), job.bucketKey(), job.promptVersion(), job.modelConfigHash(),
                job.provider(), job.model(), status, attempts, owner, leaseUntil, nextRunAt, error, job.createdAt(),
                job.updatedAt(), finishedAt);
    }
}
