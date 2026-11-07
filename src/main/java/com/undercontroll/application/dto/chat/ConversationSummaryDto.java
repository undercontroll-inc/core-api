package com.undercontroll.application.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record ConversationSummaryDto(
        String conversationId,
        String title,
        @Schema(description = "Last message, up to 160 characters") String preview,
        int messageCount,
        Instant lastMessageAt,
        Instant createdAt
) {
}
