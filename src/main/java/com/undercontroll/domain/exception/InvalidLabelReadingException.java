package com.undercontroll.domain.exception;

public class InvalidLabelReadingException extends RuntimeException {

    public static final String CODE = "LABEL_READING_INVALID";

    public InvalidLabelReadingException(String message) {
        super(message);
    }
}
