package com.undercontroll.presentation.dto;

import com.undercontroll.application.dto.OrderEnrichedDto;

import java.util.List;

public record GetAllOrdersResponse(
        List<OrderEnrichedDto> data
) {
}
