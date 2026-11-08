package com.undercontroll.application.dto.storage;

import io.swagger.v3.oas.annotations.media.Schema;

public record UploadRequest(
        @Schema(example = "image/jpeg") String contentType,
        @Schema(description = "Exact file size in bytes", example = "183442") Long sizeBytes,
        @Schema(description = "Original file name, metadata only", example = "nota.pdf") String fileName
) {
}
