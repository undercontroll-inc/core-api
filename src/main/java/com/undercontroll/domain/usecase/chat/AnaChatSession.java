package com.undercontroll.domain.usecase.chat;

import com.undercontroll.domain.gateway.AnaConversationMemory.ConversationLock;
import com.undercontroll.domain.model.chat.AnaTurnRequest;
import com.undercontroll.domain.model.chat.ConversationKey;

public record AnaChatSession(
        ConversationKey key,
        AnaTurnRequest turn,
        ConversationLock lock,
        boolean memoryAvailable,
        Long conversationPk,
        Long userMessageId
) {

    public AnaChatSession(ConversationKey key, AnaTurnRequest turn, ConversationLock lock, boolean memoryAvailable) {
        this(key, turn, lock, memoryAvailable, null, null);
    }

    public String conversationId() {
        return key.conversationId();
    }
}
