package com.undercontroll.domain.exception;

public class InvalidPasswordResetException extends RuntimeException {
    public InvalidPasswordResetException(String message) {
        super(message);
    }
}
