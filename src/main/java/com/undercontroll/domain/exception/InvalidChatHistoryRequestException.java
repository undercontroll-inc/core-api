package com.undercontroll.domain.exception;

public class InvalidChatHistoryRequestException extends RuntimeException {

    private final String field;

    public InvalidChatHistoryRequestException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
