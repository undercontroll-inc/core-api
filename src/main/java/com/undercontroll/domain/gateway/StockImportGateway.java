package com.undercontroll.domain.gateway;

import com.undercontroll.domain.enums.StockImportFailureReason;
import com.undercontroll.domain.enums.StockImportStage;
import com.undercontroll.domain.enums.StockImportStatus;
import com.undercontroll.domain.model.stockimport.StockImport;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StockImportGateway {

    StockImport create(StockImport stockImport, byte[] content);

    Optional<StockImport> findById(Long importId);

    Optional<StockImport> findByIdForUpdate(Long importId);

    List<StockImport> findRecent(StockImportStatus status, int limit);

    StockImport save(StockImport stockImport);

    Optional<byte[]> findContent(Long importId);

    List<Long> findRunnable(Instant now, int limit);

    Optional<StockImport> tryAcquire(Long importId, String owner, Instant now, Instant leaseUntil);

    void updateStage(Long importId, String owner, StockImportStage stage, Instant now);

    boolean complete(StockImport extracted, String owner, Instant now);

    boolean fail(Long importId, String owner, StockImportFailureReason reason, String message, Instant now);

    boolean requeue(Long importId, Instant now);

    Optional<Long> findConfirmedDuplicate(Long importId, String accessKey, String fileSha256);

    int purgeFiles(Instant finishedBefore);
}
