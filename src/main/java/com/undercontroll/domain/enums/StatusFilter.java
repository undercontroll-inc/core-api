package com.undercontroll.domain.enums;

import java.util.List;

public enum StatusFilter {
    ONGOING(OrderStatus.ONGOING),
    READY(List.of(OrderStatus.READY_FOR_PICKUP)),
    @Deprecated
    COMPLETED(List.of(OrderStatus.READY_FOR_PICKUP)),
    DELIVERED(List.of(OrderStatus.DELIVERED)),
    ALL(List.of(OrderStatus.values()));

    private final List<OrderStatus> statuses;

    StatusFilter(List<OrderStatus> statuses) {
        this.statuses = statuses;
    }

    public List<OrderStatus> getStatuses() {
        return statuses;
    }
}
