package com.undercontroll.domain.exception;

public class InvalidStockImportException extends RuntimeException {

    public static final String CODE = "STOCK_IMPORT_INVALID";

    public InvalidStockImportException(String message) {
        super(message);
    }
}
