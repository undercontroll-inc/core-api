package com.undercontroll.domain.enums;

import java.util.List;

public enum OrderStatus {

    RECEIVED,
    AWAITING_APPROVAL,
    IN_REPAIR,
    READY_FOR_PICKUP,
    DELIVERED;

    public static final List<OrderStatus> ONGOING = List.of(RECEIVED, AWAITING_APPROVAL, IN_REPAIR);

    public boolean ongoing() {
        return ONGOING.contains(this);
    }
}
