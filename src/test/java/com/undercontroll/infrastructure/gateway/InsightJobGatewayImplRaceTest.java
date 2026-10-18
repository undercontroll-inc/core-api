package com.undercontroll.infrastructure.gateway;

import com.undercontroll.domain.enums.InsightJobStatus;
import com.undercontroll.domain.model.insight.InsightJob;
import com.undercontroll.domain.model.insight.NewInsightJob;
import com.undercontroll.infrastructure.mapper.InsightJobPersistenceMapper;
import com.undercontroll.infrastructure.persistence.entity.InsightJobJpaEntity;
import com.undercontroll.infrastructure.persistence.repository.InsightAttemptJpaRepository;
import com.undercontroll.infrastructure.persistence.repository.InsightJobJpaRepository;
import com.undercontroll.infrastructure.persistence.repository.MarketInsightPublicationJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InsightJobGatewayImplRaceTest {

    private static final NewInsightJob JOB = new NewInsightJob(1L, "2026-09", "7", "h", "gemini", "m");

    private final InsightJobJpaRepository jobs = mock(InsightJobJpaRepository.class);
    private final InsightJobPersistenceMapper mapper = mock(InsightJobPersistenceMapper.class);
    private final InsightJobGatewayImpl gateway = new InsightJobGatewayImpl(
            jobs, mock(InsightAttemptJpaRepository.class), mock(MarketInsightPublicationJpaRepository.class), mapper);

    @Test
    @DisplayName("a concurrent insert of the same job returns the row the other replica created")
    void concurrentInsertReturnsExistingJob() {
        InsightJobJpaEntity existing = InsightJobJpaEntity.builder().id(7L).status(InsightJobStatus.PENDING).build();
        InsightJob domain = mock(InsightJob.class);
        when(jobs.findBySnapshotIdAndPromptVersionAndModelConfigHash(1L, "7", "h"))
                .thenReturn(Optional.empty(), Optional.of(existing));
        when(jobs.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq_insight_job_snapshot_config"));
        when(mapper.toDomain(existing)).thenReturn(domain);

        assertSame(domain, gateway.enqueue(JOB, Instant.EPOCH));
    }

    @Test
    @DisplayName("an integrity error that is not a duplicate job is rethrown")
    void unrelatedIntegrityErrorIsRethrown() {
        when(jobs.findBySnapshotIdAndPromptVersionAndModelConfigHash(1L, "7", "h")).thenReturn(Optional.empty());
        when(jobs.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("not null"));

        assertThrows(DataIntegrityViolationException.class, () -> gateway.enqueue(JOB, Instant.EPOCH));
    }
}
