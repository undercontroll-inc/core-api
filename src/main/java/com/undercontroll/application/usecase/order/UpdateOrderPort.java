package com.undercontroll.application.usecase.order;

import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.application.dto.PartDto;
import com.undercontroll.application.dto.UpdateOrderItemDto;

import java.util.List;

public interface UpdateOrderPort {
    record Input(
            Integer orderId,
            OrderStatus status,
            List<PartDto> parts,
            List<UpdateOrderItemDto> appliances,
            String serviceDescription
    ) {}

    record Output(
            Boolean success,
            String message
    ) {}

    Output execute(Input input);
}
