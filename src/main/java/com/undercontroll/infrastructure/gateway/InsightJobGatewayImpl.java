package com.undercontroll.infrastructure.gateway;

import com.undercontroll.domain.enums.InsightAttemptStatus;
import com.undercontroll.domain.enums.InsightJobStatus;
import com.undercontroll.domain.exception.InsightLeaseLostException;
import com.undercontroll.domain.gateway.InsightJobGateway;
import com.undercontroll.domain.gateway.InsightPublicationGateway;
import com.undercontroll.domain.model.insight.InsightAttempt;
import com.undercontroll.domain.model.insight.InsightJob;
import com.undercontroll.domain.model.insight.MarketInsightPublication;
import com.undercontroll.domain.model.insight.NewInsightJob;
import com.undercontroll.infrastructure.mapper.InsightJobPersistenceMapper;
import com.undercontroll.infrastructure.persistence.entity.InsightAttemptJpaEntity;
import com.undercontroll.infrastructure.persistence.entity.InsightJobJpaEntity;
import com.undercontroll.infrastructure.persistence.repository.InsightAttemptJpaRepository;
import com.undercontroll.infrastructure.persistence.repository.InsightJobJpaRepository;
import com.undercontroll.infrastructure.persistence.repository.MarketInsightPublicationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class InsightJobGatewayImpl implements InsightJobGateway, InsightPublicationGateway {

    private final InsightJobJpaRepository jobs;
    private final InsightAttemptJpaRepository attempts;
    private final MarketInsightPublicationJpaRepository publications;
    private final InsightJobPersistenceMapper mapper;

    @Override
    public InsightJob enqueue(NewInsightJob job, Instant now) {
        return find(job).orElseGet(() -> insert(job, now));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InsightJob> findById(long jobId) {
        return jobs.findById(jobId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InsightJob> findLatestForSnapshot(long snapshotId) {
        return jobs.findFirstBySnapshotIdOrderByCreatedAtDescIdDesc(snapshotId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findRunnable(Instant now, int limit) {
        return jobs.findRunnableIds(InsightJobStatus.PENDING, InsightJobStatus.RUNNING, now, PageRequest.of(0, limit));
    }

    @Override
    @Transactional
    public Optional<InsightJob> tryAcquire(long jobId, String owner, Instant now, Instant leaseUntil) {
        int updated = jobs.acquire(jobId, owner, now, leaseUntil, InsightJobStatus.PENDING, InsightJobStatus.RUNNING);
        if (updated == 0) {
            return Optional.empty();
        }
        return jobs.findById(jobId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public boolean reschedule(long jobId, String owner, Instant nextRunAt, String error, Instant now) {
        return jobs.release(jobId, owner, InsightJobStatus.PENDING, nextRunAt, error, now, null,
                InsightJobStatus.RUNNING) == 1;
    }

    @Override
    @Transactional
    public boolean fail(long jobId, String owner, String error, Instant now) {
        return jobs.release(jobId, owner, InsightJobStatus.FAILED, now, error, now, now,
                InsightJobStatus.RUNNING) == 1;
    }

    @Override
    @Transactional
    public boolean requeue(long jobId, Instant now) {
        return jobs.requeue(jobId, now, InsightJobStatus.PENDING, InsightJobStatus.FAILED) == 1;
    }

    @Override
    @Transactional
    public InsightAttempt saveAttempt(InsightAttempt attempt) {
        return mapper.toDomain(attempts.save(mapper.toEntity(attempt)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InsightAttempt> findLatestAttempt(long jobId) {
        return attempts.findFirstByJobIdOrderByAttemptNumberDesc(jobId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public MarketInsightPublication publish(MarketInsightPublication publication, String owner, Instant now) {
        int released = jobs.release(publication.jobId(), owner, InsightJobStatus.SUCCESS, now, null, now, now,
                InsightJobStatus.RUNNING);
        if (released == 0) {
            throw new InsightLeaseLostException(publication.jobId());
        }
        InsightAttemptJpaEntity attempt = attempts.findById(publication.attemptId())
                .orElseThrow(() -> new IllegalStateException("Insight attempt " + publication.attemptId() + " not found"));
        attempt.setStatus(InsightAttemptStatus.PUBLISHED);
        attempt.setFinishedAt(now);
        return mapper.toDomain(publications.save(mapper.toEntity(publication)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MarketInsightPublication> findLatest() {
        return publications.findFirstByOrderByPublishedAtDescIdDesc().map(mapper::toDomain);
    }

    private Optional<InsightJob> find(NewInsightJob job) {
        return jobs.findBySnapshotIdAndPromptVersionAndModelConfigHash(
                job.snapshotId(), job.promptVersion(), job.modelConfigHash()).map(mapper::toDomain);
    }

    private InsightJob insert(NewInsightJob job, Instant now) {
        try {
            return mapper.toDomain(jobs.saveAndFlush(InsightJobJpaEntity.builder()
                    .snapshotId(job.snapshotId())
                    .bucketKey(job.bucketKey())
                    .promptVersion(job.promptVersion())
                    .modelConfigHash(job.modelConfigHash())
                    .provider(job.provider())
                    .model(job.model())
                    .status(InsightJobStatus.PENDING)
                    .attemptCount(0)
                    .nextRunAt(now)
                    .createdAt(now)
                    .updatedAt(now)
                    .build()));
        } catch (DataIntegrityViolationException ex) {
            return find(job).orElseThrow(() -> ex);
        }
    }
}
