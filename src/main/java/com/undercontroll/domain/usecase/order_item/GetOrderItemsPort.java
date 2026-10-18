package com.undercontroll.domain.usecase.order_item;

import java.util.List;

public interface GetOrderItemsPort {
    List<OrderItemOutput> execute(Integer orderId);
}
