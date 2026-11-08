package com.undercontroll.application.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;

public record OrderPartDto(
        Integer id,
        String item,
        String description,
        String brand,
        Double price,
        @Schema(description = "Component stock") Long quantity,
        String supplier,
        String category,
        @Schema(description = "Quantity of this part on the order") Long demandQuantity
) {
}
