package com.undercontroll.presentation.dto;

import com.undercontroll.application.dto.PartDto;
import com.undercontroll.application.dto.UpdateOrderItemDto;
import com.undercontroll.domain.enums.OrderStatus;

import java.util.List;

public record UpdateOrderRequest(
        OrderStatus status,
        List<UpdateOrderItemDto> appliances,
        List<PartDto> parts,
        String serviceDescription
) {
}
