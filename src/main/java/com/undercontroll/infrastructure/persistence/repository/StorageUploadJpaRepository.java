package com.undercontroll.infrastructure.persistence.repository;

import com.undercontroll.domain.storage.StorageArea;
import com.undercontroll.domain.storage.UploadIntent;
import com.undercontroll.infrastructure.persistence.entity.StorageUploadJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface StorageUploadJpaRepository extends JpaRepository<StorageUploadJpaEntity, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE StorageUploadJpaEntity u SET u.status = :status, u.completedAt = :now
            WHERE u.id = :id AND u.status = :pending
            """)
    int transition(
            @Param("id") UUID id,
            @Param("status") UploadIntent.Status status,
            @Param("now") Instant now,
            @Param("pending") UploadIntent.Status pending
    );

    @Query("""
            SELECT COUNT(u) FROM StorageUploadJpaEntity u
            WHERE u.area = :area AND u.reference = :reference AND u.status = :pending AND u.expiresAt > :now
            """)
    long countPending(
            @Param("area") StorageArea area,
            @Param("reference") String reference,
            @Param("now") Instant now,
            @Param("pending") UploadIntent.Status pending
    );

    @Query("SELECT u FROM StorageUploadJpaEntity u WHERE u.status = :pending AND u.expiresAt < :before ORDER BY u.expiresAt")
    List<StorageUploadJpaEntity> findExpired(
            @Param("pending") UploadIntent.Status pending,
            @Param("before") Instant before,
            Pageable page
    );
}
