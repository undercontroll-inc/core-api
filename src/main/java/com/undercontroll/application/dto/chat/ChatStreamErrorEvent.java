package com.undercontroll.application.dto.chat;

public record ChatStreamErrorEvent(String state, String code, String message, boolean retryable) {

    public static ChatStreamErrorEvent failed(String code, String message) {
        return new ChatStreamErrorEvent("failed", code, message, true);
    }
}
