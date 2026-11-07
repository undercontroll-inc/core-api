package com.undercontroll.domain.usecase.chat;

import com.undercontroll.domain.model.chat.Conversation;
import com.undercontroll.domain.model.chat.MessagePage;

public interface ListConversationMessagesPort {

    Output execute(Input input);

    record Input(String conversationId, String cursor, Integer limit) {
    }

    record Output(Conversation conversation, MessagePage page, String version) {
    }
}
