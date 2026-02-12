package com.undercontroll.domain.exception;

public class InvalidServiceOrderException extends RuntimeException {
    public InvalidServiceOrderException(String message) {
        super(message);
    }
}
