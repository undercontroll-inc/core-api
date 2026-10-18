package com.undercontroll.domain.usecase.order_item;

import java.util.Optional;

public interface GetOrderItemByIdPort {
    Optional<OrderItemOutput> execute(Integer orderId, Integer orderItemId);
}
