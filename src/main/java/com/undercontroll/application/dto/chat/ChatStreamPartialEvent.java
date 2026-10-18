package com.undercontroll.application.dto.chat;

public record ChatStreamPartialEvent(String state, String content, String reason, boolean retryable) {

    public static ChatStreamPartialEvent of(String content, String reason) {
        return new ChatStreamPartialEvent("partial", content, reason, true);
    }
}
