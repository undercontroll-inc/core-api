package com.undercontroll.domain.exception;

import com.undercontroll.domain.enums.StockImportStatus;

public class StockImportInvalidStateException extends RuntimeException {

    public static final String CODE = "STOCK_IMPORT_INVALID_STATE";

    public StockImportInvalidStateException(Long importId, StockImportStatus status, String operation) {
        super("Stock import " + importId + " is " + status + " and cannot be " + operation);
    }
}
