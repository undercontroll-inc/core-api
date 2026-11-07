package com.undercontroll.domain.model.stockimport;

import com.undercontroll.domain.enums.StockImportSourceType;

public record InvoiceFile(byte[] content, String mimeType, StockImportSourceType sourceType) {
}
