package com.undercontroll.domain.usecase.chat;

import com.undercontroll.domain.model.chat.AnaTurnRequest;

public record StreamChatSession(AnaTurnRequest turn) {

    public String conversationId() {
        return turn.conversationId();
    }
}
