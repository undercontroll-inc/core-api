package com.undercontroll.domain.model.chat;

import java.time.Instant;

public record StoredChatMessage(
        Long id,
        int seq,
        ChatMessage.Role role,
        String content,
        AnaReply.State state,
        String reason,
        Instant createdAt
) {

    public boolean completed() {
        return state == AnaReply.State.COMPLETED;
    }
}
