package com.undercontroll.domain.usecase.stockimport.impl;

import com.undercontroll.domain.enums.StockImportStatus;
import com.undercontroll.domain.exception.StockImportInvalidStateException;
import com.undercontroll.domain.exception.StockImportNotFoundException;
import com.undercontroll.domain.gateway.StockImportGateway;
import com.undercontroll.domain.model.stockimport.StockImport;
import com.undercontroll.domain.model.stockimport.StockImportReview;
import com.undercontroll.domain.stockimport.StockImportReviews;
import com.undercontroll.domain.usecase.stockimport.UpdateStockImportPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class UpdateStockImportImpl implements UpdateStockImportPort {

    private final StockImportGateway stockImportGateway;
    private final Clock clock;

    @Override
    @Transactional
    public StockImport execute(Long importId, StockImportReview review) {
        StockImport current = stockImportGateway.findByIdForUpdate(importId)
                .orElseThrow(() -> new StockImportNotFoundException(importId));
        if (current.status() != StockImportStatus.READY) {
            throw new StockImportInvalidStateException(importId, current.status(), "updated");
        }
        StockImport reviewed = StockImportReviews.apply(current, review, false);
        return stockImportGateway.save(reviewed.toBuilder().updatedAt(clock.instant()).build());
    }
}
