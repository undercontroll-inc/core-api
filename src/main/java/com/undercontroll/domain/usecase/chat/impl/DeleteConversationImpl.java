package com.undercontroll.domain.usecase.chat.impl;

import com.undercontroll.domain.exception.ConversationMemoryUnavailableException;
import com.undercontroll.domain.gateway.AnaConversationMemory;
import com.undercontroll.domain.gateway.ConversationGateway;
import com.undercontroll.domain.gateway.ConversationListVersion;
import com.undercontroll.domain.gateway.CurrentUserIdPort;
import com.undercontroll.domain.model.chat.ConversationKey;
import com.undercontroll.domain.usecase.chat.DeleteConversationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteConversationImpl implements DeleteConversationPort {

    private final CurrentUserIdPort currentUserIdPort;
    private final ConversationGateway conversations;
    private final ConversationListVersion listVersion;
    private final AnaConversationMemory memory;
    private final Clock clock;

    @Override
    public void execute(String conversationId) {
        ConversationKey key = ConversationRequests.key(currentUserIdPort.require(), conversationId);
        if (!conversations.softDelete(key.userId(), key.conversationId(), clock.instant())) {
            return;
        }
        listVersion.bump(key.userId());
        try {
            memory.clear(key);
        } catch (ConversationMemoryUnavailableException ex) {
            log.warn("Ana memory clear failed conversationId={}", key.conversationId());
        }
        log.info("Conversation deleted userId={} conversationId={}", key.userId(), key.conversationId());
    }
}
