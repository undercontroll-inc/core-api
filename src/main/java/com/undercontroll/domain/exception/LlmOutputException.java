package com.undercontroll.domain.exception;

public class LlmOutputException extends RuntimeException {

    public LlmOutputException(String message) {
        super(message);
    }

    public LlmOutputException(String message, Throwable cause) {
        super(message, cause);
    }
}
