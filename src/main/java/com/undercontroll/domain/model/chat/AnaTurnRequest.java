package com.undercontroll.domain.model.chat;

import java.util.List;

public record AnaTurnRequest(
        String conversationId,
        String message,
        String briefing,
        List<ChatMessage> history,
        boolean webSearch
) {

    public AnaTurnRequest {
        history = history == null ? List.of() : List.copyOf(history);
    }
}
