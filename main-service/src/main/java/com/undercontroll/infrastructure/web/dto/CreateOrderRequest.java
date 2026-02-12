package com.undercontroll.infrastructure.web.dto;

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
