package com.undercontroll.domain.exception;

public class ConversationMemoryUnavailableException extends RuntimeException {

    public ConversationMemoryUnavailableException(Throwable cause) {
        super("Ana conversation memory is unavailable", cause);
    }
}
