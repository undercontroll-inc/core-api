package com.undercontroll.domain.exception;

public class ServiceOrderNotFoundException extends RuntimeException {
    public ServiceOrderNotFoundException(String message) {
        super(message);
    }
}
