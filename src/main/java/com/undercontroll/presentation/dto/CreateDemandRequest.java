package com.undercontroll.presentation.dto;

public record CreateDemandRequest(
        Integer componentPartId,
        Long quantity,
        Integer orderId
) {
}
