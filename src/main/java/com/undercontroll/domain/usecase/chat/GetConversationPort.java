package com.undercontroll.domain.usecase.chat;

import com.undercontroll.domain.model.chat.Conversation;

public interface GetConversationPort {

    Conversation execute(String conversationId);
}
