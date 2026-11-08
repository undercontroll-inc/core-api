package com.undercontroll.application.dto.order;

import com.undercontroll.domain.enums.OrderPhotoKind;
import io.swagger.v3.oas.annotations.media.Schema;

public record PhotoUploadRequest(
        OrderPhotoKind kind,
        @Schema(example = "image/jpeg") String contentType,
        @Schema(description = "Exact file size in bytes", example = "183442") Long sizeBytes
) {
}
