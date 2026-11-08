package com.undercontroll.domain.usecase.order.impl;

import com.undercontroll.domain.usecase.order.GetOrderByIdPort;
import com.undercontroll.domain.usecase.order.OrderOutput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetOrderByIdImpl implements GetOrderByIdPort {

    private final OrderAccess orderAccess;
    private final OrderDetailsAssembler orderDetailsAssembler;

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderOutput> execute(Integer orderId) {
        boolean includeTechnical = orderAccess.administrator();
        return orderAccess.findVisible(orderId)
                .map(order -> orderDetailsAssembler.detail(order, includeTechnical));
    }
}
