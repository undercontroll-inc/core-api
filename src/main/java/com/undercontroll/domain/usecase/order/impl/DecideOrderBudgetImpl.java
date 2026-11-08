package com.undercontroll.domain.usecase.order.impl;

import com.undercontroll.domain.enums.OrderActor;
import com.undercontroll.domain.exception.InvalidOrderTransitionException;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.usecase.order.DecideOrderBudgetPort;
import com.undercontroll.domain.usecase.order.OrderOutput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DecideOrderBudgetImpl implements DecideOrderBudgetPort {

    private final OrderLifecycleService lifecycle;
    private final OrderDetailsAssembler assembler;
    private final OrderGateway orderGateway;

    @Override
    public OrderOutput execute(Input input) {
        if (input.decision() == null) {
            throw new InvalidOrderTransitionException("Informe a decisão: APPROVED ou DECLINED");
        }
        Order order = lifecycle.decide(input.orderId(), input.decision(), OrderActor.SHOP, input.note());
        return assembler.detail(orderGateway.findById(order.getId()).orElse(order), true);
    }
}
