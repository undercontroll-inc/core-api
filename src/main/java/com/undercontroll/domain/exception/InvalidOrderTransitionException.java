package com.undercontroll.domain.exception;

public class InvalidOrderTransitionException extends RuntimeException {

    public static final String CODE = "ORDER_INVALID_TRANSITION";

    public InvalidOrderTransitionException(String message) {
        super(message);
    }
}
