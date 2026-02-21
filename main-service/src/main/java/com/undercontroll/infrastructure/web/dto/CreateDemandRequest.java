package com.undercontroll.infrastructure.web.dto;

import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.model.Order;

public record CreateDemandRequest(
        ComponentPart componentPart,
        Long quantity,
        Order order
) {
}
