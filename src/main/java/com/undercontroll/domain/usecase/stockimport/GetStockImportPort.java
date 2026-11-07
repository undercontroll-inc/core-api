package com.undercontroll.domain.usecase.stockimport;

import com.undercontroll.domain.model.stockimport.StockImport;

public interface GetStockImportPort {

    StockImport execute(Long importId);
}
