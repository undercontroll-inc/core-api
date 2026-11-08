package com.undercontroll.domain.exception;

public class StorageUnavailableException extends RuntimeException {

    public static final String CODE = "STORAGE_UNAVAILABLE";

    public StorageUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
