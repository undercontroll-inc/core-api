package com.undercontroll.domain.exception;

public class TempFileException extends RuntimeException {

    public TempFileException(String message) {
        super(message);
    }

    public TempFileException(String message, Throwable cause) {
        super(message, cause);
    }
}

