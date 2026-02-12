package com.undercontroll.infrastructure.web.dto;

import com.undercontroll.domain.entity.enums.OrderStatus;

import java.util.List;

public record OrdersByStatusResponse(
    List<StatusCount> statusCounts
) {
    public record StatusCount(
        OrderStatus status,
        Long count
    ) {}
}

