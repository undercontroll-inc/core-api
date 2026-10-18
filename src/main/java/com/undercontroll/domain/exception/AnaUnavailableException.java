package com.undercontroll.domain.exception;

public class AnaUnavailableException extends RuntimeException {

    public static final String CODE = "CHAT_UNAVAILABLE";
    public static final String MESSAGE = "Ana is currently unavailable.";
    public static final String TIMEOUT_CODE = "CHAT_TIMEOUT";
    public static final String TIMEOUT_MESSAGE = "Ana took too long to answer. Try again.";

    public AnaUnavailableException() {
        super(MESSAGE);
    }

    public AnaUnavailableException(Throwable cause) {
        super(MESSAGE, cause);
    }
}
