package com.undercontroll.infrastructure.scheduling;

import com.undercontroll.domain.gateway.StockImportDispatcher;
import com.undercontroll.domain.gateway.ObjectStorageGateway;
import com.undercontroll.domain.gateway.StockImportGateway;
import com.undercontroll.domain.model.stockimport.StockImport;
import com.undercontroll.domain.storage.StorageArea;
import com.undercontroll.domain.usecase.stockimport.ProcessStockImportsPort;
import com.undercontroll.infrastructure.logging.MdcKeys;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class StockImportWorker implements StockImportDispatcher {

    static final int PURGE_BATCH = 200;

    private final ProcessStockImportsPort processStockImportsPort;
    private final StockImportGateway stockImportGateway;
    private final ObjectStorageGateway storage;
    private final Executor executor;
    private final Clock clock;
    private final Duration fileRetention;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean pending = new AtomicBoolean(false);

    public StockImportWorker(
            ProcessStockImportsPort processStockImportsPort,
            StockImportGateway stockImportGateway,
            ObjectStorageGateway storage,
            @Qualifier("taskExecutor") Executor executor,
            Clock clock,
            @Value("${undercontroll.ai.stock-import.file-retention:30d}") Duration fileRetention
    ) {
        this.processStockImportsPort = processStockImportsPort;
        this.stockImportGateway = stockImportGateway;
        this.storage = storage;
        this.executor = executor;
        this.clock = clock;
        this.fileRetention = fileRetention;
    }

    @Override
    public void dispatch() {
        pending.set(true);
        try {
            executor.execute(this::drain);
        } catch (TaskRejectedException ex) {
            log.warn("Stock import dispatch rejected; the scheduled sweep will pick it up");
        }
    }

    @Scheduled(
            fixedDelayString = "${undercontroll.ai.stock-import.worker-interval:5s}",
            initialDelayString = "${undercontroll.ai.stock-import.worker-initial-delay:20s}"
    )
    public void work() {
        pending.set(true);
        drain();
    }

    @Scheduled(cron = "${undercontroll.ai.stock-import.purge-cron:0 30 3 * * *}")
    public void purge() {
        try {
            Instant before = clock.instant().minus(fileRetention);
            int purged = stockImportGateway.purgeLegacyFiles(before);
            for (StockImport stockImport : stockImportGateway.findStoredFilesToPurge(before, PURGE_BATCH)) {
                storage.delete(StorageArea.STOCK_IMPORTS, stockImport.storageKey());
                stockImportGateway.markFilePurged(stockImport.id(), clock.instant());
                purged++;
            }
            if (purged > 0) {
                log.info("Stock import purged {} stored file(s)", purged);
            }
        } catch (RuntimeException ex) {
            log.warn("Stock import file purge failed: {}", ex.toString());
        }
    }

    void drain() {
        while (pending.get() && running.compareAndSet(false, true)) {
            MDC.put(MdcKeys.CORRELATION_ID, "stock-import-" + UUID.randomUUID());
            try {
                pending.set(false);
                int processed;
                do {
                    processed = processStockImportsPort.execute();
                } while (processed > 0);
            } catch (RuntimeException ex) {
                log.warn("Stock import worker failed: {}", ex.toString());
            } finally {
                running.set(false);
                MDC.clear();
            }
        }
    }
}
