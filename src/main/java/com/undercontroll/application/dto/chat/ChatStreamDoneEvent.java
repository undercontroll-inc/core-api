package com.undercontroll.application.dto.chat;

public record ChatStreamDoneEvent(String state, String content, Long messageId) {

    public static ChatStreamDoneEvent completed(String content) {
        return completed(content, null);
    }

    public static ChatStreamDoneEvent completed(String content, Long messageId) {
        return new ChatStreamDoneEvent("completed", content, messageId);
    }
}
