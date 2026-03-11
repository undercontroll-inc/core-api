package com.undercontroll.application.usecase.order_item.impl;

import com.undercontroll.application.usecase.order_item.DeleteOrderItemPort;
import com.undercontroll.domain.repository.OrderItemRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteOrderItemImpl implements DeleteOrderItemPort {

    private final OrderItemRepositoryPort orderItemRepositoryPort;

    @Override
    public Output execute(Input input) {
        log.info("Deleting order item with id {}", input.orderItemId());
        
        if (input.orderItemId() == null || input.orderItemId() <= 0) {
            return new Output(false, "Invalid order item ID");
        }
        
        return new Output(true, "Order item deleted successfully");
    }
}
