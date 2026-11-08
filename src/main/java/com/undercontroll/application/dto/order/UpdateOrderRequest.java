package com.undercontroll.application.dto.order;

import com.undercontroll.application.dto.orderitem.UpdateOrderItemDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record UpdateOrderRequest(
        @Schema(description = "Not accepted anymore: use POST /v1/api/orders/{id}/transitions", deprecated = true) Object status,
        List<UpdateOrderItemDto> appliances,
        List<PartDto> parts,
        String customerDescription,
        String technicalDescription
) {
}
