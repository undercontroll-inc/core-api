package com.undercontroll.application.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;

public record LabelReadingResponse(
        boolean isLabel,
        String type,
        String brand,
        String model,
        String serial,
        @Schema(allowableValues = {"127V", "220V", "BIVOLT"}) String voltage,
        double confidence
) {
}
