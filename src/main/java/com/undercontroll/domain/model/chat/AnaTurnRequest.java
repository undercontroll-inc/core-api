package com.undercontroll.domain.model.chat;

import com.undercontroll.domain.support.Deadline;

import java.util.List;

public record AnaTurnRequest(
        String conversationId,
        String message,
        String briefing,
        List<ChatMessage> history,
        boolean webSearch,
        Deadline deadline
) {

    public AnaTurnRequest {
        history = history == null ? List.of() : List.copyOf(history);
    }
}
