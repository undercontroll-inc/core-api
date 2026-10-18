package com.undercontroll.domain.usecase.chat;

import com.undercontroll.domain.gateway.AnaConversationMemory.ConversationLock;
import com.undercontroll.domain.model.chat.AnaTurnRequest;
import com.undercontroll.domain.model.chat.ConversationKey;

public record AnaChatSession(
        ConversationKey key,
        AnaTurnRequest turn,
        ConversationLock lock,
        boolean memoryAvailable
) {

    public String conversationId() {
        return key.conversationId();
    }
}
