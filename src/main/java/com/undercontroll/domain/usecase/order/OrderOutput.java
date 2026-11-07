package com.undercontroll.domain.usecase.order;

import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.usecase.component.ComponentOutput;
import com.undercontroll.domain.usecase.order_item.OrderItemOutput;
import com.undercontroll.domain.usecase.user.UserOutput;

import java.util.List;

public record OrderOutput(
        Integer id,
        UserOutput user,
        List<OrderItemOutput> appliances,
        List<ComponentOutput> parts,
        Double partsTotal,
        Double laborTotal,
        Double discount,
        Double totalValue,
        String receivedAt,
        String deadline,
        String nf,
        boolean haveReturnGuarantee,
        String customerDescription,
        String technicalDescription,
        OrderStatus status,
        String updatedAt
) {

    public static OrderOutput from(Order order, boolean includeTechnical) {
        return new OrderOutput(
                order.getId(),
                UserOutput.from(order.getUser()),
                order.getOrderItems().stream().map(OrderItemOutput::from).toList(),
                order.getDemands().stream().map(demand -> ComponentOutput.from(demand.getComponent())).toList(),
                order.calculatePartsTotal(),
                order.calculateLaborTotal(),
                order.getDiscount(),
                order.getTotal(),
                order.getReceived_at() != null ? order.getReceived_at().toString() : null,
                null,
                order.getNf(),
                order.isReturnGuarantee(),
                order.getCustomerDescription(),
                includeTechnical ? order.getTechnicalDescription() : null,
                order.getStatus(),
                order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : null
        );
    }
}
