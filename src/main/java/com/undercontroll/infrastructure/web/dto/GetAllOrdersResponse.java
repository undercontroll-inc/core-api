package com.undercontroll.infrastructure.web.dto;

import com.undercontroll.application.dto.OrderEnrichedDto;

import java.util.List;

public record GetAllOrdersResponse(
        List<OrderEnrichedDto> data
) {
}
