package com.undercontroll.application.dto.chat;

public record ChatStreamDoneEvent(String state, String content) {

    public static ChatStreamDoneEvent completed(String content) {
        return new ChatStreamDoneEvent("completed", content);
    }
}
