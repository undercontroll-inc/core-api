package com.undercontroll.application.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SendChatMessageRequest(

        @NotBlank
        @Size(max = 500)
        String content,

        @Schema(description = "Client-chosen conversation id (letters, digits, - or _). Omit to use the default conversation.",
                example = "kitchen-2026-10-17")
        @Pattern(regexp = "^[A-Za-z0-9_-]{1,64}$")
        String conversationId

) {
}
