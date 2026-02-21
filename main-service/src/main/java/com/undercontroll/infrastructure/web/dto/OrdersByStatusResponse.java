package com.undercontroll.infrastructure.web.dto;

import com.undercontroll.domain.model.enums.OrderStatus;

import java.util.List;

public record OrdersByStatusResponse(
    List<StatusCount> statusCounts
) {
    public record StatusCount(
        OrderStatus status,
        Long count
    ) {}
}

