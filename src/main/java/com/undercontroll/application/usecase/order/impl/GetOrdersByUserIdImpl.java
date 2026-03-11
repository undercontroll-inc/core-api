package com.undercontroll.application.usecase.order.impl;

import com.undercontroll.application.usecase.order.GetOrdersByUserIdPort;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.repository.OrderRepositoryPort;
import com.undercontroll.application.dto.GetOrdersByUserIdResponse;
import com.undercontroll.application.dto.OrderEnrichedDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetOrdersByUserIdImpl implements GetOrdersByUserIdPort {

    private final OrderRepositoryPort orderRepositoryPort;

    @Override
    public Output execute(Input input) {
        List<Order> orders = orderRepositoryPort.findByUserId(input.userId());
        List<OrderEnrichedDto> orderDtos = orders.stream()
                .map(o -> new OrderEnrichedDto(
                        o.getId(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        o.getDiscount(),
                        o.getTotal(),
                        o.getReceived_at() != null ? o.getReceived_at().toString() : null,
                        null,
                        o.getNf(),
                        o.isReturnGuarantee(),
                        o.getDescription(),
                        null,
                        o.getStatus(),
                        o.getUpdatedAt() != null ? o.getUpdatedAt().toString() : null
                ))
                .toList();
        return new Output(new GetOrdersByUserIdResponse(orderDtos));
    }
}
