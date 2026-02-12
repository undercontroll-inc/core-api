package com.undercontroll.infrastructure.web.dto;

public record CreateOrderItemRequest(
        String brand,
        String model,
        String type,
        String imageUrl,
        String observation,
        String volt,
        String series,
        Double laborValue
) {}