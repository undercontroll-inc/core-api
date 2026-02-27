package com.undercontroll.infrastructure.web.dto;

public record CreateDemandRequest(
        Integer componentPartId,
        Long quantity,
        Integer orderId
) {
}
