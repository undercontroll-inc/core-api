package com.undercontroll.infrastructure.web.dto;

public record OrderItemCreateOrderRequest(
        String type,
        String brand,
        String model,
        String voltage,
        String serial,
        String customerNote,
        Double laborValue
) {
}
