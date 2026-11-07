package com.undercontroll.domain.exception;

public class ConversationBusyException extends RuntimeException {

    public static final String CODE = "CHAT_CONVERSATION_BUSY";
    public static final String MESSAGE = "Ana is still answering the previous message in this conversation.";

    public ConversationBusyException() {
        super(MESSAGE);
    }
}
