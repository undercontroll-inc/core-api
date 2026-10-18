package com.undercontroll.infrastructure.persistence.repository;

import com.undercontroll.domain.enums.InsightJobStatus;
import com.undercontroll.infrastructure.persistence.entity.InsightJobJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface InsightJobJpaRepository extends JpaRepository<InsightJobJpaEntity, Long> {

    Optional<InsightJobJpaEntity> findBySnapshotIdAndPromptVersionAndModelConfigHash(
            Long snapshotId,
            String promptVersion,
            String modelConfigHash
    );

    Optional<InsightJobJpaEntity> findFirstBySnapshotIdOrderByCreatedAtDescIdDesc(Long snapshotId);

    @Query("""
            SELECT j.id FROM InsightJobJpaEntity j
            WHERE (j.status = :pending AND j.nextRunAt <= :now)
               OR (j.status = :running AND j.leaseUntil < :now)
            ORDER BY j.nextRunAt ASC, j.id ASC
            """)
    List<Long> findRunnableIds(
            @Param("pending") InsightJobStatus pending,
            @Param("running") InsightJobStatus running,
            @Param("now") Instant now,
            Pageable page
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE InsightJobJpaEntity j
            SET j.status = :running, j.leaseOwner = :owner, j.leaseUntil = :leaseUntil,
                j.attemptCount = j.attemptCount + 1, j.updatedAt = :now
            WHERE j.id = :id
              AND ((j.status = :pending AND j.nextRunAt <= :now)
                OR (j.status = :running AND j.leaseUntil < :now))
            """)
    int acquire(
            @Param("id") Long id,
            @Param("owner") String owner,
            @Param("now") Instant now,
            @Param("leaseUntil") Instant leaseUntil,
            @Param("pending") InsightJobStatus pending,
            @Param("running") InsightJobStatus running
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE InsightJobJpaEntity j
            SET j.status = :status, j.leaseOwner = NULL, j.leaseUntil = NULL, j.nextRunAt = :nextRunAt,
                j.lastError = :error, j.updatedAt = :now, j.finishedAt = :finishedAt
            WHERE j.id = :id AND j.status = :running AND j.leaseOwner = :owner
            """)
    int release(
            @Param("id") Long id,
            @Param("owner") String owner,
            @Param("status") InsightJobStatus status,
            @Param("nextRunAt") Instant nextRunAt,
            @Param("error") String error,
            @Param("now") Instant now,
            @Param("finishedAt") Instant finishedAt,
            @Param("running") InsightJobStatus running
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE InsightJobJpaEntity j
            SET j.status = :pending, j.attemptCount = 0, j.nextRunAt = :now, j.lastError = NULL,
                j.updatedAt = :now, j.finishedAt = NULL
            WHERE j.id = :id AND j.status = :failed
            """)
    int requeue(
            @Param("id") Long id,
            @Param("now") Instant now,
            @Param("pending") InsightJobStatus pending,
            @Param("failed") InsightJobStatus failed
    );
}
