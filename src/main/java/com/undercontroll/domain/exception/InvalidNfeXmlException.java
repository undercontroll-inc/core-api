package com.undercontroll.domain.exception;

public class InvalidNfeXmlException extends RuntimeException {

    public InvalidNfeXmlException(String message) {
        super(message);
    }

    public InvalidNfeXmlException(String message, Throwable cause) {
        super(message, cause);
    }
}
