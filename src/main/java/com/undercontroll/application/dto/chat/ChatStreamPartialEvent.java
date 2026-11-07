package com.undercontroll.application.dto.chat;

public record ChatStreamPartialEvent(String state, String content, String reason, boolean retryable, Long messageId) {

    public static ChatStreamPartialEvent of(String content, String reason) {
        return of(content, reason, null);
    }

    public static ChatStreamPartialEvent of(String content, String reason, Long messageId) {
        return new ChatStreamPartialEvent("partial", content, reason, true, messageId);
    }
}
