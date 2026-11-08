package com.undercontroll.infrastructure.gateway;

import com.undercontroll.domain.gateway.UploadIntentGateway;
import com.undercontroll.domain.storage.StorageArea;
import com.undercontroll.domain.storage.UploadIntent;
import com.undercontroll.infrastructure.persistence.entity.StorageUploadJpaEntity;
import com.undercontroll.infrastructure.persistence.repository.StorageUploadJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UploadIntentGatewayImpl implements UploadIntentGateway {

    private final StorageUploadJpaRepository uploads;

    @Override
    @Transactional
    public UploadIntent save(UploadIntent intent) {
        return toDomain(uploads.save(StorageUploadJpaEntity.builder()
                .id(intent.id())
                .area(intent.area())
                .objectKey(intent.objectKey())
                .ownerUserId(intent.ownerUserId())
                .reference(intent.reference())
                .kind(intent.kind())
                .originalName(intent.originalName())
                .contentType(intent.contentType())
                .sizeBytes(intent.sizeBytes())
                .status(intent.status())
                .expiresAt(intent.expiresAt())
                .createdAt(intent.createdAt())
                .completedAt(intent.completedAt())
                .build()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UploadIntent> findById(UUID id) {
        return uploads.findById(id).map(UploadIntentGatewayImpl::toDomain);
    }

    @Override
    @Transactional
    public boolean markCompleted(UUID id, Instant now) {
        return uploads.transition(id, UploadIntent.Status.COMPLETED, now, UploadIntent.Status.PENDING) == 1;
    }

    @Override
    @Transactional
    public void markRejected(UUID id, Instant now) {
        uploads.transition(id, UploadIntent.Status.REJECTED, now, UploadIntent.Status.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPending(StorageArea area, String reference, Instant now) {
        return uploads.countPending(area, reference, now, UploadIntent.Status.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UploadIntent> findExpiredPending(Instant before, int limit) {
        return uploads.findExpired(UploadIntent.Status.PENDING, before, PageRequest.of(0, limit)).stream()
                .map(UploadIntentGatewayImpl::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        uploads.deleteById(id);
    }

    private static UploadIntent toDomain(StorageUploadJpaEntity entity) {
        return new UploadIntent(entity.getId(), entity.getArea(), entity.getObjectKey(), entity.getOwnerUserId(),
                entity.getReference(), entity.getKind(), entity.getOriginalName(), entity.getContentType(),
                entity.getSizeBytes(), entity.getStatus(), entity.getExpiresAt(), entity.getCreatedAt(),
                entity.getCompletedAt());
    }
}
