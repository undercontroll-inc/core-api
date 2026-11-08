package com.undercontroll.application.dto.orderitem;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.time.LocalDateTime;

public record UpdateOrderItemRequest(
        String imageUrl,
        Double labor,
        @JsonAlias("volt") String voltage,
        @JsonAlias("series") String serial,
        String type,
        String brand,
        String model,
        LocalDateTime completedAt
) {
}
