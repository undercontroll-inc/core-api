package com.undercontroll.application.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;

public record TrackingLinkResponse(
        String trackingUrl,
        @Schema(description = "Short pt-BR text with the link, ready for wa.me") String whatsappText
) {
}
