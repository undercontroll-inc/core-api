package com.undercontroll.domain.exception;

public class StockImportUnavailableException extends RuntimeException {

    public static final String CODE = "STOCK_IMPORT_UNAVAILABLE";

    public StockImportUnavailableException() {
        super("Invoice extraction is currently unavailable; NF-e XML uploads still work.");
    }
}
