package com.undercontroll.infrastructure.web.dto;

import java.util.List;

public record GetAllOrdersResponse(
        List<OrderEnrichedDto> data
) {
}
