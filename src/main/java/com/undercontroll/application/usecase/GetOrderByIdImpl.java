package com.undercontroll.application.usecase;

import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.port.in.GetOrderByIdPort;
import com.undercontroll.domain.port.out.OrderRepositoryPort;
import com.undercontroll.application.dto.GetOrderByIdResponse;
import com.undercontroll.application.dto.OrderEnrichedDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetOrderByIdImpl implements GetOrderByIdPort {

    private final OrderRepositoryPort orderRepositoryPort;

    @Override
    public Output execute(Input input) {
        Optional<Order> order = orderRepositoryPort.findById(input.orderId());
        if (order.isEmpty()) {
            return new Output(null);
        }
        Order o = order.get();
        OrderEnrichedDto orderDto = new OrderEnrichedDto(
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
        );
        return new Output(new GetOrderByIdResponse(orderDto));
    }
}
