package com.undercontroll.domain.exception;

public class InvalidDeleteOrderException extends RuntimeException {
    public InvalidDeleteOrderException(String message) {
        super(message);
    }
}
