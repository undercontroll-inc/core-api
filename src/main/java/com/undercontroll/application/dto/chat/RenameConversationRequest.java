package com.undercontroll.application.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;

public record RenameConversationRequest(
        @Schema(description = "1 to 120 characters", example = "Orçamento do liquidificador") String title
) {
}
