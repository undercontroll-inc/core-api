package com.undercontroll.application.dto.storage;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Presigned upload. PUT the raw file bytes to uploadUrl with exactly these headers, then call the matching complete endpoint")
public record UploadTicketDto(
        UUID uploadId,
        @Schema(description = "Presigned URL, valid until expiresAt") String uploadUrl,
        @Schema(example = "PUT") String method,
        @Schema(description = "Headers that are part of the signature; send them unchanged",
                example = "{\"Content-Type\": \"image/jpeg\", \"Content-Length\": \"183442\"}") Map<String, String> headers,
        Instant expiresAt
) {
}
