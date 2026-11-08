package com.undercontroll.application.dto.orderitem;

import com.fasterxml.jackson.annotation.JsonAlias;
public record UpdateOrderItemDto(
        Integer id,
        String type,
        String brand,
        String model,
        @JsonAlias("volt") String voltage,
        @JsonAlias("series") String serial,
        Double laborValue
) {
}
