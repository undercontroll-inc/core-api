package com.undercontroll.application.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;

public record SendChatMessageResponse(
        String conversationId,
        String content,
        @Schema(allowableValues = {"completed", "partial"}) String state,
        @Schema(description = "Why the reply is partial, e.g. max_tokens; null when completed") String reason,
        @Schema(description = "Stored id of the user message") Long userMessageId,
        @Schema(description = "Stored id of the reply; null when it could not be stored") Long assistantMessageId
) {

    public SendChatMessageResponse(String conversationId, String content, String state, String reason) {
        this(conversationId, content, state, reason, null, null);
    }
}
