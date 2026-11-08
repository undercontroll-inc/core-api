package com.undercontroll.domain.storage;

import java.time.Instant;
import java.util.UUID;

public record UploadIntent(
        UUID id,
        StorageArea area,
        String objectKey,
        Integer ownerUserId,
        String reference,
        String kind,
        String originalName,
        String contentType,
        long sizeBytes,
        Status status,
        Instant expiresAt,
        Instant createdAt,
        Instant completedAt
) {

    public enum Status {
        PENDING,
        COMPLETED,
        REJECTED
    }
}
