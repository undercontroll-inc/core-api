package com.undercontroll.domain.exception;

public class ConversationNotFoundException extends RuntimeException {

    public static final String CODE = "CONVERSATION_NOT_FOUND";

    public ConversationNotFoundException(String conversationId) {
        super("Conversation " + conversationId + " not found");
    }
}
