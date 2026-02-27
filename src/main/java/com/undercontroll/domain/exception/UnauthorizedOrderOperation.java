package com.undercontroll.domain.exception;

public class UnauthorizedOrderOperation extends RuntimeException {
    public UnauthorizedOrderOperation(String message) {
        super(message);
    }
}
