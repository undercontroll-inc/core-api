package com.undercontroll.domain.usecase.stockimport;

import com.undercontroll.domain.enums.StockImportStatus;
import com.undercontroll.domain.model.stockimport.StockImport;

import java.util.List;

public interface ListStockImportsPort {

    List<StockImport> execute(StockImportStatus status, Integer limit);
}
