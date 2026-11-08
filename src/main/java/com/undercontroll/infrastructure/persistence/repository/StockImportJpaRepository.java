package com.undercontroll.infrastructure.persistence.repository;

import com.undercontroll.domain.enums.StockImportFailureReason;
import com.undercontroll.domain.enums.StockImportStage;
import com.undercontroll.domain.enums.StockImportStatus;
import com.undercontroll.infrastructure.persistence.entity.StockImportJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockImportJpaRepository extends JpaRepository<StockImportJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM StockImportJpaEntity i WHERE i.id = :id")
    Optional<StockImportJpaEntity> findByIdForUpdate(@Param("id") Long id);

    Optional<StockImportJpaEntity> findByStorageKey(String storageKey);

    @Query("""
            SELECT i FROM StockImportJpaEntity i
            WHERE i.storageKey IS NOT NULL AND i.filePurgedAt IS NULL
              AND i.status IN :statuses AND i.updatedAt < :before
            ORDER BY i.updatedAt
            """)
    List<StockImportJpaEntity> findStoredFilesToPurge(
            @Param("statuses") Collection<StockImportStatus> statuses,
            @Param("before") Instant before,
            Pageable page
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE StockImportJpaEntity i SET i.filePurgedAt = :now WHERE i.id = :id")
    int markFilePurged(@Param("id") Long id, @Param("now") Instant now);

    @Query("SELECT i FROM StockImportJpaEntity i ORDER BY i.createdAt DESC, i.id DESC")
    List<StockImportJpaEntity> findRecent(Pageable page);

    @Query("SELECT i FROM StockImportJpaEntity i WHERE i.status = :status ORDER BY i.createdAt DESC, i.id DESC")
    List<StockImportJpaEntity> findRecentByStatus(@Param("status") StockImportStatus status, Pageable page);

    @Query("""
            SELECT i.id FROM StockImportJpaEntity i
            WHERE i.status = :processing AND i.nextRunAt <= :now
              AND (i.leaseUntil IS NULL OR i.leaseUntil < :now)
            ORDER BY i.nextRunAt ASC, i.id ASC
            """)
    List<Long> findRunnableIds(
            @Param("processing") StockImportStatus processing,
            @Param("now") Instant now,
            Pageable page
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE StockImportJpaEntity i
            SET i.leaseOwner = :owner, i.leaseUntil = :leaseUntil,
                i.attemptCount = i.attemptCount + 1, i.updatedAt = :now
            WHERE i.id = :id AND i.status = :processing AND i.nextRunAt <= :now
              AND (i.leaseUntil IS NULL OR i.leaseUntil < :now)
            """)
    int acquire(
            @Param("id") Long id,
            @Param("owner") String owner,
            @Param("now") Instant now,
            @Param("leaseUntil") Instant leaseUntil,
            @Param("processing") StockImportStatus processing
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE StockImportJpaEntity i
            SET i.stage = :stage, i.updatedAt = :now
            WHERE i.id = :id AND i.status = :processing AND i.leaseOwner = :owner
            """)
    int updateStage(
            @Param("id") Long id,
            @Param("owner") String owner,
            @Param("stage") StockImportStage stage,
            @Param("now") Instant now,
            @Param("processing") StockImportStatus processing
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE StockImportJpaEntity i
            SET i.status = :failed, i.stage = NULL, i.failureReason = :reason, i.failureMessage = :message,
                i.leaseOwner = NULL, i.leaseUntil = NULL, i.updatedAt = :now, i.finishedAt = :now
            WHERE i.id = :id AND i.status = :processing AND i.leaseOwner = :owner
            """)
    int fail(
            @Param("id") Long id,
            @Param("owner") String owner,
            @Param("reason") StockImportFailureReason reason,
            @Param("message") String message,
            @Param("now") Instant now,
            @Param("failed") StockImportStatus failed,
            @Param("processing") StockImportStatus processing
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE StockImportJpaEntity i
            SET i.status = :processing, i.stage = :received, i.failureReason = NULL, i.failureMessage = NULL,
                i.attemptCount = 0, i.nextRunAt = :now, i.leaseOwner = NULL, i.leaseUntil = NULL,
                i.updatedAt = :now, i.finishedAt = NULL
            WHERE i.id = :id AND i.status = :failed
            """)
    int requeue(
            @Param("id") Long id,
            @Param("now") Instant now,
            @Param("processing") StockImportStatus processing,
            @Param("received") StockImportStage received,
            @Param("failed") StockImportStatus failed
    );

    @Query("""
            SELECT i.id FROM StockImportJpaEntity i
            WHERE i.status = :confirmed AND i.id <> :id
              AND ((:accessKey IS NOT NULL AND i.accessKey = :accessKey) OR i.fileSha256 = :sha)
            ORDER BY i.confirmedAt DESC
            """)
    List<Long> findConfirmedDuplicates(
            @Param("id") Long id,
            @Param("accessKey") String accessKey,
            @Param("sha") String sha,
            @Param("confirmed") StockImportStatus confirmed,
            Pageable page
    );
}
