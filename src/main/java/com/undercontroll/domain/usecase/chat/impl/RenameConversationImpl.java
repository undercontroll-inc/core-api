package com.undercontroll.domain.usecase.chat.impl;

import com.undercontroll.domain.exception.ConversationNotFoundException;
import com.undercontroll.domain.exception.InvalidChatHistoryRequestException;
import com.undercontroll.domain.gateway.ConversationGateway;
import com.undercontroll.domain.gateway.ConversationListVersion;
import com.undercontroll.domain.gateway.CurrentUserIdPort;
import com.undercontroll.domain.model.chat.Conversation;
import com.undercontroll.domain.model.chat.ConversationTitles;
import com.undercontroll.domain.usecase.chat.RenameConversationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class RenameConversationImpl implements RenameConversationPort {

    private final CurrentUserIdPort currentUserIdPort;
    private final ConversationGateway conversations;
    private final ConversationListVersion listVersion;
    private final Clock clock;

    @Override
    public Conversation execute(Input input) {
        String id = ConversationRequests.id(input.conversationId());
        String title = input.title() == null ? "" : input.title().strip().replaceAll("\\s+", " ");
        if (title.isEmpty() || title.length() > ConversationTitles.MAX_TITLE) {
            throw new InvalidChatHistoryRequestException("title",
                    "title must have between 1 and " + ConversationTitles.MAX_TITLE + " characters");
        }
        Integer userId = currentUserIdPort.require();
        Conversation renamed = conversations.rename(userId, id, title, clock.instant())
                .orElseThrow(() -> new ConversationNotFoundException(id));
        listVersion.bump(userId);
        return renamed;
    }
}
