package com.undercontroll.application.dto;

import java.util.List;

public record GetOrdersByUserIdResponse(
        List<OrderEnrichedDto> data
) {
}
