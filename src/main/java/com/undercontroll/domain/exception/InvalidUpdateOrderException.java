package com.undercontroll.domain.exception;

public class InvalidUpdateOrderException extends RuntimeException {
    public InvalidUpdateOrderException(String message) {
        super(message);
    }
}
