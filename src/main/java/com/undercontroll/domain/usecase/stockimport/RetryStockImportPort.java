package com.undercontroll.domain.usecase.stockimport;

import com.undercontroll.domain.model.stockimport.StockImport;

public interface RetryStockImportPort {

    StockImport execute(Long importId);
}
