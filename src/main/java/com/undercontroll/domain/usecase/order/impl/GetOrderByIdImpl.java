package com.undercontroll.domain.usecase.order.impl;

import com.undercontroll.domain.gateway.CurrentUserAdminPort;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.usecase.order.GetOrderByIdPort;
import com.undercontroll.domain.usecase.order.OrderOutput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetOrderByIdImpl implements GetOrderByIdPort {

    private final OrderGateway orderGateway;
    private final CurrentUserAdminPort currentUserAdminPort;

    @Override
    public Optional<OrderOutput> execute(Integer orderId) {
        boolean includeTechnical = currentUserAdminPort.isAdministrator();
        return orderGateway.findById(orderId)
                .map(order -> OrderOutput.from(order, includeTechnical));
    }
}
