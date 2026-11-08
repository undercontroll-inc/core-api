package com.undercontroll.domain.gateway;

import com.undercontroll.domain.storage.StorageArea;
import com.undercontroll.domain.storage.UploadIntent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UploadIntentGateway {

    UploadIntent save(UploadIntent intent);

    Optional<UploadIntent> findById(UUID id);

    boolean markCompleted(UUID id, Instant now);

    void markRejected(UUID id, Instant now);

    long countPending(StorageArea area, String reference, Instant now);

    List<UploadIntent> findExpiredPending(Instant before, int limit);

    void delete(UUID id);
}
