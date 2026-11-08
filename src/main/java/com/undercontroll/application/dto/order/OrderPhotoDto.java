package com.undercontroll.application.dto.order;

import com.undercontroll.domain.enums.OrderPhotoKind;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record OrderPhotoDto(
        Long id,
        OrderPhotoKind kind,
        @Schema(description = "Presigned URL, valid for 15 minutes") String url,
        Instant createdAt
) {
}
