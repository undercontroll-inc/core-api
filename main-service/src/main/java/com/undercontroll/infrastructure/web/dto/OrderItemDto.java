package com.undercontroll.infrastructure.web.dto;

import java.time.LocalDateTime;

public record OrderItemDto(
    Integer id,
    String imageUrl,
    String model,
    String type,
    String brand,
    String observation,
    String volt,
    String series,
    Double laborValue,
    LocalDateTime completedAt
) {
}
