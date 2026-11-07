package com.undercontroll.domain.usecase.chat;

import com.undercontroll.domain.model.chat.Conversation;

public interface RenameConversationPort {

    Conversation execute(Input input);

    record Input(String conversationId, String title) {
    }
}
