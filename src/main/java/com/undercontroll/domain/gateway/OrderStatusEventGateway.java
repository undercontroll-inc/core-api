package com.undercontroll.domain.gateway;

import com.undercontroll.domain.model.OrderStatusEvent;

import java.util.List;

public interface OrderStatusEventGateway {

    OrderStatusEvent save(OrderStatusEvent event);

    List<OrderStatusEvent> findByOrderId(Integer orderId);
}
