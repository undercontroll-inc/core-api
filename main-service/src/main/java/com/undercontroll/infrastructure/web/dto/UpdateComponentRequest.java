package com.undercontroll.infrastructure.web.dto;

public record UpdateComponentRequest(
        String item,
        String description,
        String brand,
        Double price,
        String supplier,
        String category
) {
}
