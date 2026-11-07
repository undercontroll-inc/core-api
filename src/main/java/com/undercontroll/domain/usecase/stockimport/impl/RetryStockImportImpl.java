package com.undercontroll.domain.usecase.stockimport.impl;

import com.undercontroll.domain.enums.StockImportStatus;
import com.undercontroll.domain.exception.StockImportInvalidStateException;
import com.undercontroll.domain.exception.StockImportNotFoundException;
import com.undercontroll.domain.gateway.StockImportDispatcher;
import com.undercontroll.domain.gateway.StockImportGateway;
import com.undercontroll.domain.model.stockimport.StockImport;
import com.undercontroll.domain.usecase.stockimport.RetryStockImportPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class RetryStockImportImpl implements RetryStockImportPort {

    private final StockImportGateway stockImportGateway;
    private final StockImportDispatcher stockImportDispatcher;
    private final Clock clock;

    @Override
    public StockImport execute(Long importId) {
        StockImport current = stockImportGateway.findById(importId)
                .orElseThrow(() -> new StockImportNotFoundException(importId));
        if (current.status() != StockImportStatus.FAILED || !stockImportGateway.requeue(importId, clock.instant())) {
            throw new StockImportInvalidStateException(importId, current.status(), "retried");
        }
        stockImportDispatcher.dispatch();
        return stockImportGateway.findById(importId).orElseThrow(() -> new StockImportNotFoundException(importId));
    }
}
