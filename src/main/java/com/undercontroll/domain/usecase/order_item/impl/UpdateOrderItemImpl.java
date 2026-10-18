package com.undercontroll.domain.usecase.order_item.impl;

import com.undercontroll.domain.usecase.order_item.UpdateOrderItemPort;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.OrderItem;
import com.undercontroll.domain.exception.InvalidOrderItemException;
import com.undercontroll.domain.exception.OrderItemNotFoundException;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.gateway.OrderItemGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateOrderItemImpl implements UpdateOrderItemPort {

    private final OrderItemGateway orderItemGateway;
    private final OrderGateway orderGateway;

    @Override
    public void execute(Integer orderId, Integer orderItemId, Input request) {
        if (orderItemId == null) {
            throw new InvalidOrderItemException("Order item ID cannot be null for update");
        }
        if (request.labor() != null && request.labor() < 0) {
            throw new InvalidOrderItemException("Order item labor cannot be negative");
        }

        requireItemBelongsToOrder(orderId, orderItemId);

        OrderItem orderFound = orderItemGateway.findById(orderItemId)
                .orElseThrow(() -> new OrderItemNotFoundException(
                        "Could not find the order item for update with id: %s".formatted(orderItemId)
                ));

        if (request.imageUrl() != null) {
            orderFound.setImageUrl(request.imageUrl());
        }
        if (request.volt() != null) {
            orderFound.setVolt(request.volt());
        }
        if (request.series() != null) {
            orderFound.setSeries(request.series());
        }
        if (request.completedAt() != null) {
            orderFound.setCompletedAt(request.completedAt());
        }
        if (request.labor() != null) {
            orderFound.setLaborValue(request.labor());
        }
        if (request.type() != null) {
            orderFound.setType(request.type());
        }
        if (request.brand() != null) {
            orderFound.setBrand(request.brand());
        }
        if (request.model() != null) {
            orderFound.setModel(request.model());
        }

        orderItemGateway.save(orderFound);
        log.info("Order item updated orderId={} itemId={}", orderId, orderItemId);
    }

    private void requireItemBelongsToOrder(Integer orderId, Integer orderItemId) {
        Order order = orderGateway.findOrderByOrderItemId(orderItemId)
                .orElseThrow(() -> new OrderItemNotFoundException(
                        "Could not find the order item with id: %s".formatted(orderItemId)
                ));
        if (!orderId.equals(order.getId())) {
            throw new OrderItemNotFoundException(
                    "Order item %s does not belong to order %s".formatted(orderItemId, orderId)
            );
        }
    }
}
