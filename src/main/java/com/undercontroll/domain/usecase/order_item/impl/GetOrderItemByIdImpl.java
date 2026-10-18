package com.undercontroll.domain.usecase.order_item.impl;

import com.undercontroll.domain.usecase.order_item.GetOrderItemByIdPort;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.usecase.order_item.OrderItemOutput;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.gateway.OrderItemGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetOrderItemByIdImpl implements GetOrderItemByIdPort {

    private final OrderItemGateway orderItemGateway;
    private final OrderGateway orderGateway;

    @Override
    public Optional<OrderItemOutput> execute(Integer orderId, Integer orderItemId) {
        Optional<Order> owner = orderGateway.findOrderByOrderItemId(orderItemId);
        if (owner.isEmpty() || !orderId.equals(owner.get().getId())) {
            return Optional.empty();
        }
        return orderItemGateway.findById(orderItemId)
                .map(OrderItemOutput::from);
    }
}
