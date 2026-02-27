package com.undercontroll.infrastructure.web.dto;

public record RegisterComponentResponse(
        String name,
        String description,
        String brand,
        Double price,
        String supplier,
        String category
) {
}
