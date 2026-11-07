package com.undercontroll.domain.exception;

public class StockImportNotFoundException extends RuntimeException {

    public static final String CODE = "STOCK_IMPORT_NOT_FOUND";

    public StockImportNotFoundException(Long importId) {
        super("Stock import " + importId + " not found");
    }
}
