package com.undercontroll.application.dto.orderitem;

import com.fasterxml.jackson.annotation.JsonAlias;
public record CreateOrderItemRequest(
        String brand,
        String model,
        String type,
        String imageUrl,
        @JsonAlias("volt") String voltage,
        @JsonAlias("series") String serial,
        Double laborValue
) {}
