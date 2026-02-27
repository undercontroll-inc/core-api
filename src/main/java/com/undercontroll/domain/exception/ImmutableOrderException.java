package com.undercontroll.domain.exception;

public class ImmutableOrderException extends RuntimeException {
    public ImmutableOrderException(String message) {
        super(message);
    }
}

