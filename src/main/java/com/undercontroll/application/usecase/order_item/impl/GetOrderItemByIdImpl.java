package com.undercontroll.application.usecase.order_item.impl;

import com.undercontroll.application.usecase.order_item.GetOrderItemByIdPort;
import com.undercontroll.domain.model.OrderItem;
import com.undercontroll.domain.repository.OrderItemRepositoryPort;
import com.undercontroll.application.dto.OrderItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetOrderItemByIdImpl implements GetOrderItemByIdPort {

    private final OrderItemRepositoryPort orderItemRepositoryPort;

    @Override
    public Output execute(Input input) {
        Optional<OrderItem> orderItem = orderItemRepositoryPort.findById(input.orderItemId());
        if (orderItem.isEmpty()) {
            return new Output(null);
        }
        OrderItem oi = orderItem.get();
        return new Output(new OrderItemDto(
                oi.getId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));
    }
}
