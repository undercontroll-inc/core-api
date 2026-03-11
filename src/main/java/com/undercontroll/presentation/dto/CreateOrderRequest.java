package com.undercontroll.presentation.dto;

import com.undercontroll.application.dto.OrderItemCreateOrderRequest;
import com.undercontroll.application.dto.PartDto;

import java.util.List;

public record CreateOrderRequest(

        Integer userId,
        List<OrderItemCreateOrderRequest> appliances,
        List<PartDto> parts,
        Double discount,
        String receivedAt,
        String deadline,
        String serviceDescription,
        String notes,
        String status,
        boolean returnGuarantee,
        boolean fabricGuarantee,
        String nf

) {
}
