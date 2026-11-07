package com.undercontroll.application.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ConversationPageDto(
        List<ConversationSummaryDto> items,
        @Schema(description = "Opaque cursor for the next (older) page; null on the last page") String nextCursor
) {
}
