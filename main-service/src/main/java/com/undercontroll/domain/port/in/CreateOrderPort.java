package com.undercontroll.domain.port.in;

import com.undercontroll.domain.entity.Order;
import com.undercontroll.infrastructure.web.dto.PartDto;
import com.undercontroll.infrastructure.web.dto.OrderItemCreateOrderRequest;

import java.util.List;

public interface CreateOrderPort {
    record Input(
            Integer userId,
            List<PartDto> parts,
            List<OrderItemCreateOrderRequest> appliances,
            Double discount,
            String deadline,
            String receivedAt,
            String nf,
            Boolean fabricGuarantee,
            Boolean returnGuarantee,
            String serviceDescription
    ) {}

    record Output(
            Integer id,
            Integer userId,
            String status,
            Double total
    ) {}

    Output execute(Input input);
}
