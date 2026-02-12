package com.undercontroll.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representação de uma demanda")
public record DemandDto(
        @Schema(description = "ID da demanda", example = "25")
        Integer id,
        @Schema(description = "ID do componente", example = "10")
        Integer componentId,
        @Schema(description = "ID do pedido", example = "5")
        Integer orderId,
        @Schema(description = "Quantidade demandada", example = "3")
        Long quantity
) {}

