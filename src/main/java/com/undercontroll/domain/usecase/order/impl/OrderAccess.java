package com.undercontroll.domain.usecase.order.impl;

import com.undercontroll.domain.exception.OrderNotFoundException;
import com.undercontroll.domain.gateway.CurrentUserAdminPort;
import com.undercontroll.domain.gateway.CurrentUserIdPort;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderAccess {

    private final OrderGateway orderGateway;
    private final CurrentUserIdPort currentUserIdPort;
    private final CurrentUserAdminPort currentUserAdminPort;

    public boolean administrator() {
        return currentUserAdminPort.isAdministrator();
    }

    public Optional<Order> findVisible(Integer orderId) {
        Optional<Order> order = orderGateway.findById(orderId);
        if (order.isEmpty() || administrator()) {
            return order;
        }
        Integer userId = currentUserIdPort.require();
        return order.filter(found -> found.getUser() != null && userId.equals(found.getUser().getId()));
    }

    public Order requireVisible(Integer orderId) {
        return findVisible(orderId).orElseThrow(() -> notFound(orderId));
    }

    public Order require(Integer orderId) {
        return orderGateway.findById(orderId).orElseThrow(() -> notFound(orderId));
    }

    static OrderNotFoundException notFound(Integer orderId) {
        return new OrderNotFoundException("Order " + orderId + " not found");
    }
}
