package com.undercontroll.infrastructure.persistence.entity;

import com.undercontroll.domain.storage.StorageArea;
import com.undercontroll.domain.storage.UploadIntent;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "storage_upload",
        uniqueConstraints = @UniqueConstraint(name = "uq_storage_upload_key", columnNames = "object_key"),
        indexes = {
                @Index(name = "ix_storage_upload_pending", columnList = "status, expires_at"),
                @Index(name = "ix_storage_upload_reference", columnList = "area, reference, status")
        }
)
public class StorageUploadJpaEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StorageArea area;

    @Column(name = "object_key", nullable = false)
    private String objectKey;

    @Column(name = "owner_user_id", nullable = false)
    private Integer ownerUserId;

    @Column(length = 64)
    private String reference;

    @Column(length = 20)
    private String kind;

    @Column(name = "original_name")
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UploadIntent.Status status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
