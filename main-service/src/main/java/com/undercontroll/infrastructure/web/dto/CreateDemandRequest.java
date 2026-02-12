package com.undercontroll.infrastructure.web.dto;

import com.undercontroll.domain.entity.ComponentPart;
import com.undercontroll.domain.entity.Order;

public record CreateDemandRequest(
        ComponentPart componentPart,
        Long quantity,
        Order order
) {
}
