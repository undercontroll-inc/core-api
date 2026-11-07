package com.undercontroll.domain.usecase.stockimport.impl;

import com.undercontroll.domain.enums.StockImportStatus;
import com.undercontroll.domain.gateway.StockImportGateway;
import com.undercontroll.domain.model.stockimport.StockImport;
import com.undercontroll.domain.usecase.stockimport.ListStockImportsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListStockImportsImpl implements ListStockImportsPort {

    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 50;

    private final StockImportGateway stockImportGateway;

    @Override
    public List<StockImport> execute(StockImportStatus status, Integer limit) {
        int effective = limit == null ? DEFAULT_LIMIT : Math.clamp(limit, 1, MAX_LIMIT);
        return stockImportGateway.findRecent(status, effective);
    }
}
