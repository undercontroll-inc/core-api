package com.undercontroll.domain.usecase.stockimport;

import com.undercontroll.domain.model.stockimport.StockImport;

import java.util.UUID;

public interface CreateStockImportPort {

    StockImport execute(UUID uploadId);
}
