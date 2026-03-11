package com.undercontroll.application.usecase.order;

import com.undercontroll.application.dto.PartDto;
import com.undercontroll.application.dto.OrderItemCreateOrderRequest;

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
