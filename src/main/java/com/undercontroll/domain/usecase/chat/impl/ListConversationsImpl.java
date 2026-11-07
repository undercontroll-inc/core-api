package com.undercontroll.domain.usecase.chat.impl;

import com.undercontroll.domain.gateway.ConversationListVersion;
import com.undercontroll.domain.gateway.CurrentUserIdPort;
import com.undercontroll.domain.model.chat.ChatCursor;
import com.undercontroll.domain.model.chat.ConversationPage;
import com.undercontroll.domain.usecase.chat.ListConversationsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListConversationsImpl implements ListConversationsPort {

    static final int DEFAULT_LIMIT = 20;

    private final CurrentUserIdPort currentUserIdPort;
    private final ConversationListVersion listVersion;
    private final ConversationReadQuery readQuery;

    @Override
    public ConversationPage execute(Input input) {
        int limit = ConversationRequests.limit(input.limit(), DEFAULT_LIMIT);
        ChatCursor.parseConversations(input.cursor());
        Integer userId = currentUserIdPort.require();
        return readQuery.conversations(userId, listVersion.current(userId), input.cursor(), limit);
    }
}
