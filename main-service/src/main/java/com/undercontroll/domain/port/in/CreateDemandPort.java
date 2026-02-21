package com.undercontroll.domain.port.in;

import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.model.Order;

public interface CreateDemandPort {
    record Input(
            ComponentPart componentPart,
            Long quantity,
            Order order
    ) {}

    record Output(
            Integer id,
            Integer componentId,
            Integer orderId,
            Long quantity
    ) {}

    Output execute(Input input);
}
