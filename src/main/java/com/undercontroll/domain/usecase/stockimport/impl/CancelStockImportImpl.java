package com.undercontroll.domain.usecase.stockimport.impl;

import com.undercontroll.domain.enums.StockImportStatus;
import com.undercontroll.domain.exception.StockImportInvalidStateException;
import com.undercontroll.domain.exception.StockImportNotFoundException;
import com.undercontroll.domain.gateway.StockImportGateway;
import com.undercontroll.domain.model.stockimport.StockImport;
import com.undercontroll.domain.usecase.stockimport.CancelStockImportPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Slf4j
@Service
@RequiredArgsConstructor
public class CancelStockImportImpl implements CancelStockImportPort {

    private final StockImportGateway stockImportGateway;
    private final Clock clock;

    @Override
    @Transactional
    public void execute(Long importId) {
        StockImport current = stockImportGateway.findByIdForUpdate(importId)
                .orElseThrow(() -> new StockImportNotFoundException(importId));
        if (current.status() == StockImportStatus.CANCELED) {
            return;
        }
        if (current.status() == StockImportStatus.CONFIRMED) {
            throw new StockImportInvalidStateException(importId, current.status(), "canceled");
        }
        stockImportGateway.save(current.toBuilder()
                .status(StockImportStatus.CANCELED)
                .stage(null)
                .updatedAt(clock.instant())
                .build());
        log.info("Stock import canceled id={} previousStatus={}", importId, current.status());
    }
}
