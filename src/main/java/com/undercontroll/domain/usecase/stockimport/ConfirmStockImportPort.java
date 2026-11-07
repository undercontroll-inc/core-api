package com.undercontroll.domain.usecase.stockimport;

import com.undercontroll.domain.model.stockimport.StockImport;
import com.undercontroll.domain.model.stockimport.StockImportReview;

public interface ConfirmStockImportPort {

    StockImport execute(Long importId, StockImportReview review);
}
