package com.undercontroll.domain.entity.enums;

import java.util.List;

public enum StatusFilter {
    ONGOING(List.of(OrderStatus.PENDING, OrderStatus.IN_ANALYSIS)),
    COMPLETED(List.of(OrderStatus.COMPLETED)),
    DELIVERED(List.of(OrderStatus.DELIVERED)),
    ALL(List.of(OrderStatus.PENDING, OrderStatus.IN_ANALYSIS, OrderStatus.COMPLETED, OrderStatus.DELIVERED));

    private final List<OrderStatus> statuses;

    StatusFilter(List<OrderStatus> statuses) {
        this.statuses = statuses;
    }

    public List<OrderStatus> getStatuses() {
        return statuses;
    }
}

