package com.undercontroll.application.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ConversationMessagesDto(
        String conversationId,
        @Schema(description = "Oldest first within the page") List<ChatMessageDto> items,
        @Schema(description = "Opaque cursor for older messages; null when there are none") String nextCursor
) {
}
