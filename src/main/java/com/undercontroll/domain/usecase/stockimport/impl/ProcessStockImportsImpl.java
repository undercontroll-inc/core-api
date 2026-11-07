package com.undercontroll.domain.usecase.stockimport.impl;

import com.undercontroll.domain.gateway.StockImportGateway;
import com.undercontroll.domain.model.stockimport.StockImport;
import com.undercontroll.domain.usecase.stockimport.ProcessStockImportsPort;
import com.undercontroll.domain.usecase.stockimport.RunStockImportPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProcessStockImportsImpl implements ProcessStockImportsPort {

    static final int BATCH_SIZE = 3;

    private final StockImportGateway stockImportGateway;
    private final RunStockImportPort runStockImportPort;
    private final Duration leaseDuration;
    private final Clock clock;
    private final String owner = "stock-import-" + UUID.randomUUID();

    public ProcessStockImportsImpl(
            StockImportGateway stockImportGateway,
            RunStockImportPort runStockImportPort,
            @Value("${undercontroll.ai.stock-import.lease-duration:3m}") Duration leaseDuration,
            Clock clock
    ) {
        this.stockImportGateway = stockImportGateway;
        this.runStockImportPort = runStockImportPort;
        this.leaseDuration = leaseDuration;
        this.clock = clock;
    }

    @Override
    public int execute() {
        int processed = 0;
        for (Long importId : stockImportGateway.findRunnable(clock.instant(), BATCH_SIZE)) {
            Instant now = clock.instant();
            Optional<StockImport> leased = stockImportGateway.tryAcquire(importId, owner, now, now.plus(leaseDuration));
            if (leased.isEmpty()) {
                continue;
            }
            runStockImportPort.execute(leased.get(), owner);
            processed++;
        }
        return processed;
    }
}
