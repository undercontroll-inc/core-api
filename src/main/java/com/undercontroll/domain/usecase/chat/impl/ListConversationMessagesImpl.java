package com.undercontroll.domain.usecase.chat.impl;

import com.undercontroll.domain.exception.ConversationNotFoundException;
import com.undercontroll.domain.gateway.ConversationGateway;
import com.undercontroll.domain.gateway.CurrentUserIdPort;
import com.undercontroll.domain.model.chat.ChatCursor;
import com.undercontroll.domain.model.chat.Conversation;
import com.undercontroll.domain.model.chat.MessagePage;
import com.undercontroll.domain.usecase.chat.ListConversationMessagesPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class ListConversationMessagesImpl implements ListConversationMessagesPort {

    static final int DEFAULT_LIMIT = 50;

    private final CurrentUserIdPort currentUserIdPort;
    private final ConversationGateway conversations;
    private final ConversationReadQuery readQuery;

    @Override
    public Output execute(Input input) {
        int limit = ConversationRequests.limit(input.limit(), DEFAULT_LIMIT);

        Integer beforeSeq = ChatCursor.parseMessages(input.cursor());

        String id = ConversationRequests.id(input.conversationId());

        Conversation conversation = conversations.findActive(currentUserIdPort.require(), id)
                .orElseThrow(() -> new ConversationNotFoundException(id));

        MessagePage page = beforeSeq != null && beforeSeq > conversation.lastSeq() + 1
                ? new MessagePage(new ArrayList<>(), null)
                : readQuery.messages(conversation.id(), conversation.lastSeq(), beforeSeq, limit);

        String version = conversation.id() + "-" + conversation.lastSeq() + "-"
                + (beforeSeq == null ? "latest" : beforeSeq) + "-" + limit;

        return new Output(conversation, page, version);
    }
}
