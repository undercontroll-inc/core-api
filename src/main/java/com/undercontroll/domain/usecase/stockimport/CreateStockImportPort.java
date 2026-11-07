package com.undercontroll.domain.usecase.stockimport;

import com.undercontroll.domain.model.stockimport.InvoiceUpload;
import com.undercontroll.domain.model.stockimport.StockImport;

public interface CreateStockImportPort {

    StockImport execute(InvoiceUpload upload);
}
