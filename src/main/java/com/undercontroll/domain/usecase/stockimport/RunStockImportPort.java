package com.undercontroll.domain.usecase.stockimport;

import com.undercontroll.domain.model.stockimport.StockImport;

public interface RunStockImportPort {

    void execute(StockImport leased, String owner);
}
