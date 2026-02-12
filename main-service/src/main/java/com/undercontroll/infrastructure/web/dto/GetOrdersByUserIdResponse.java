package com.undercontroll.infrastructure.web.dto;

import java.util.List;

public record GetOrdersByUserIdResponse(
        List<OrderEnrichedDto> data
) {
}
