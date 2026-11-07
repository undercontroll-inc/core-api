package com.undercontroll.domain.model.dashboard;

import com.undercontroll.domain.enums.OrderStatus;

public record StatusCount(OrderStatus status, Long count) {
}
