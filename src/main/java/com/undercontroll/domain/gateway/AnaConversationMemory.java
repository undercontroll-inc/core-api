package com.undercontroll.domain.gateway;

import com.undercontroll.domain.model.chat.ChatMessage;
import com.undercontroll.domain.model.chat.ConversationKey;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface AnaConversationMemory {

    List<ChatMessage> load(ConversationKey key);

    void append(ConversationKey key, ChatMessage user, ChatMessage assistant);

    Optional<ConversationLock> tryLock(ConversationKey key, Duration ttl);

    interface ConversationLock {

        void release();
    }
}
