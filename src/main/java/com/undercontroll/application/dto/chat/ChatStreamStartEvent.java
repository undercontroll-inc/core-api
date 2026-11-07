package com.undercontroll.application.dto.chat;

public record ChatStreamStartEvent(String conversationId, Long userMessageId) {

    public ChatStreamStartEvent(String conversationId) {
        this(conversationId, null);
    }
}
