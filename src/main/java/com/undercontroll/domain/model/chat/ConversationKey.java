package com.undercontroll.domain.model.chat;

public record ConversationKey(Integer userId, String conversationId) {

    public static final String DEFAULT_ID = "default";

    public static ConversationKey of(Integer userId, String conversationId) {
        String id = conversationId == null || conversationId.isBlank() ? DEFAULT_ID : conversationId.trim();
        return new ConversationKey(userId, id);
    }
}
