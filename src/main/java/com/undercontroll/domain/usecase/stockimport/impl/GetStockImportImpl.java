package com.undercontroll.domain.usecase.stockimport.impl;

import com.undercontroll.domain.exception.StockImportNotFoundException;
import com.undercontroll.domain.gateway.StockImportGateway;
import com.undercontroll.domain.model.stockimport.StockImport;
import com.undercontroll.domain.usecase.stockimport.GetStockImportPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetStockImportImpl implements GetStockImportPort {

    private final StockImportGateway stockImportGateway;

    @Override
    public StockImport execute(Long importId) {
        return stockImportGateway.findById(importId)
                .orElseThrow(() -> new StockImportNotFoundException(importId));
    }
}
