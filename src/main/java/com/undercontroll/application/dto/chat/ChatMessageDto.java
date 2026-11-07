package com.undercontroll.application.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record ChatMessageDto(
        Long id,
        @Schema(allowableValues = {"user", "assistant"}) String role,
        String content,
        @Schema(allowableValues = {"completed", "partial"}) String state,
        @Schema(description = "Why the reply is partial (max_tokens, canceled, ...); null when completed") String reason,
        Instant createdAt
) {
}
