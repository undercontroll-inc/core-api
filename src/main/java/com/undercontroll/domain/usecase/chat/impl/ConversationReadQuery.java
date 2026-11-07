package com.undercontroll.domain.usecase.chat.impl;

import com.undercontroll.domain.gateway.ConversationGateway;
import com.undercontroll.domain.model.chat.ChatCursor;
import com.undercontroll.domain.model.chat.Conversation;
import com.undercontroll.domain.model.chat.ConversationPage;
import com.undercontroll.domain.model.chat.MessagePage;
import com.undercontroll.domain.model.chat.StoredChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ConversationReadQuery {

    public static final String CONVERSATION_PAGES = "chatConversationPages";
    public static final String MESSAGE_PAGES = "chatMessagePages";

    private final ConversationGateway conversations;

    @Cacheable(
            value = CONVERSATION_PAGES,
            key = "#userId + ':v' + #version + ':' + (#cursor == null ? '' : #cursor) + ':' + #limit",
            condition = "#version >= 0"
    )
    public ConversationPage conversations(Integer userId, long version, String cursor, int limit) {
        List<Conversation> found = conversations.list(userId, ChatCursor.parseConversations(cursor), limit + 1);
        boolean more = found.size() > limit;
        List<Conversation> items = new ArrayList<>(more ? found.subList(0, limit) : found);
        return new ConversationPage(items, more ? ChatCursor.conversations(items.getLast()) : null);
    }

    @Cacheable(
            value = MESSAGE_PAGES,
            key = "#conversationPk + ':' + (#beforeSeq == null ? 'latest:' + #lastSeq : #beforeSeq) + ':' + #limit"
    )
    public MessagePage messages(Long conversationPk, int lastSeq, Integer beforeSeq, int limit) {
        List<StoredChatMessage> newestFirst = conversations.messagesBefore(conversationPk, beforeSeq, limit + 1);
        boolean more = newestFirst.size() > limit;
        List<StoredChatMessage> items = new ArrayList<>(more ? newestFirst.subList(0, limit) : newestFirst);
        Collections.reverse(items);
        return new MessagePage(items, more ? ChatCursor.messages(items.getFirst().seq()) : null);
    }
}
