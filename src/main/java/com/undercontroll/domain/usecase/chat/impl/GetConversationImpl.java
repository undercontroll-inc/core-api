package com.undercontroll.domain.usecase.chat.impl;

import com.undercontroll.domain.exception.ConversationNotFoundException;
import com.undercontroll.domain.gateway.ConversationGateway;
import com.undercontroll.domain.gateway.CurrentUserIdPort;
import com.undercontroll.domain.model.chat.Conversation;
import com.undercontroll.domain.usecase.chat.GetConversationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetConversationImpl implements GetConversationPort {

    private final CurrentUserIdPort currentUserIdPort;
    private final ConversationGateway conversations;

    @Override
    public Conversation execute(String conversationId) {
        String id = ConversationRequests.id(conversationId);
        return conversations.findActive(currentUserIdPort.require(), id)
                .orElseThrow(() -> new ConversationNotFoundException(id));
    }
}
