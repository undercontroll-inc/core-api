package com.undercontroll.application.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;

public record SendChatMessageResponse(
        String content,
        @Schema(allowableValues = {"completed", "partial"}) String state,
        @Schema(description = "Why the reply is partial, e.g. max_tokens; null when completed") String reason
) {
}
