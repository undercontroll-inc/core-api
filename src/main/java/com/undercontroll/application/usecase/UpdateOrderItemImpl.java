package com.undercontroll.application.usecase;

import com.undercontroll.domain.port.in.UpdateOrderItemPort;
import com.undercontroll.domain.model.OrderItem;
import com.undercontroll.domain.exception.InvalidOrderItemException;
import com.undercontroll.domain.exception.OrderItemNotFoundException;
import com.undercontroll.domain.port.out.OrderItemRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UpdateOrderItemImpl implements UpdateOrderItemPort {

    private final OrderItemRepositoryPort orderItemRepositoryPort;

    @Override
    public Output execute(Input input) {
        validateUpdateOrderItem(input);

        Optional<OrderItem> orderItem = orderItemRepositoryPort.findById(input.orderItemId());

        if (orderItem.isEmpty()) {
            throw new OrderItemNotFoundException("Could not found the order for update with id: %s".formatted(input.orderItemId()));
        }

        OrderItem orderFound = orderItem.get();

        if (input.imageUrl() != null) {
            orderFound.setImageUrl(input.imageUrl());
        }
        if (input.observation() != null) {
            orderFound.setObservation(input.observation());
        }
        if (input.volt() != null) {
            orderFound.setVolt(input.volt());
        }
        if (input.series() != null) {
            orderFound.setSeries(input.series());
        }
        if (input.completedAt() != null) {
            orderFound.setCompletedAt(input.completedAt());
        }
        if (input.labor() != null) {
            orderFound.setLaborValue(input.labor());
        }
        if (input.type() != null) {
            orderFound.setType(input.type());
        }
        if (input.brand() != null) {
            orderFound.setBrand(input.brand());
        }
        if (input.model() != null) {
            orderFound.setModel(input.model());
        }

        orderItemRepositoryPort.save(orderFound);

        return new Output(true, "Order item updated successfully");
    }

    private void validateUpdateOrderItem(Input input) {
        if (input.orderItemId() == null) {
            throw new InvalidOrderItemException("Order item ID cannot be null for update");
        }

        if (input.labor() != null && input.labor() < 0) {
            throw new InvalidOrderItemException("Order item labor cannot be negative");
        }
    }
}
