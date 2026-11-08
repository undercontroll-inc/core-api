package com.undercontroll.domain.usecase.order.impl;

import com.undercontroll.domain.enums.OrderActor;
import com.undercontroll.domain.exception.InvalidOrderTransitionException;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.usecase.order.OrderOutput;
import com.undercontroll.domain.usecase.order.TransitionOrderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransitionOrderImpl implements TransitionOrderPort {

    private final OrderLifecycleService lifecycle;
    private final OrderDetailsAssembler assembler;
    private final OrderGateway orderGateway;

    @Override
    public OrderOutput execute(Input input) {
        if (input.to() == null) {
            throw new InvalidOrderTransitionException("Informe o novo status em 'to'");
        }
        Order order = lifecycle.transition(input.orderId(), input.to(), input.outcome(), OrderActor.SHOP, input.note());
        return assembler.detail(orderGateway.findById(order.getId()).orElse(order), true);
    }
}
