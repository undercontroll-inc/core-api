package com.undercontroll.domain.usecase.chat;

import com.undercontroll.domain.model.chat.ConversationPage;

public interface ListConversationsPort {

    ConversationPage execute(Input input);

    record Input(String cursor, Integer limit) {
    }
}
