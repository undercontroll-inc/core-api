package com.undercontroll.application.dto;

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
