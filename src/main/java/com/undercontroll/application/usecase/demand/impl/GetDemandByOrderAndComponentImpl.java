package com.undercontroll.application.usecase.demand.impl;

import com.undercontroll.application.usecase.demand.GetDemandByOrderAndComponentPort;
import com.undercontroll.domain.model.Demand;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.repository.DemandRepositoryPort;
import com.undercontroll.domain.repository.OrderRepositoryPort;
import com.undercontroll.application.dto.DemandDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetDemandByOrderAndComponentImpl implements GetDemandByOrderAndComponentPort {

    private final DemandRepositoryPort demandRepositoryPort;
    private final OrderRepositoryPort orderRepositoryPort;

    @Override
    public Output execute(Input input) {
        Optional<Order> orderOpt = orderRepositoryPort.findById(input.orderId());
        if (orderOpt.isEmpty()) {
            return new Output(null);
        }
        Order order = orderOpt.get();
        Optional<Demand> demand = demandRepositoryPort.findByOrderAndComponentId(order, input.componentId());
        if (demand.isEmpty()) {
            return new Output(null);
        }
        Demand d = demand.get();
        return new Output(new DemandDto(
                d.getId(),
                input.componentId(),
                input.orderId(),
                d.getQuantity()
        ));
    }
}
