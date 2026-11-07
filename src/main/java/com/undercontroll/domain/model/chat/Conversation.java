package com.undercontroll.domain.model.chat;

import java.time.Instant;

public record Conversation(
        Long id,
        Integer userId,
        String publicId,
        String title,
        TitleSource titleSource,
        String preview,
        int messageCount,
        int lastSeq,
        Instant createdAt,
        Instant updatedAt,
        Instant lastMessageAt
) {

    public enum TitleSource {
        AUTO,
        USER
    }
}
