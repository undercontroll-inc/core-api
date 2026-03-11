package com.undercontroll.application.dto;

import com.undercontroll.domain.enums.OrderStatus;

import java.util.List;

public record OrderEnrichedDto(
        Integer id,
        UserDto user,
        List<OrderItemDto> appliances,
        List<ComponentDto> parts,
        Double partsTotal,
        Double laborTotal,
        Double discount,
        Double totalValue,
        String receivedAt,
        String deadline,
        String nf,
        boolean haveReturnGuarantee,
        String serviceDescription,
        String notes,
        OrderStatus status,
        String updatedAt
) {
}
