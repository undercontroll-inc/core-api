package com.undercontroll.domain.usecase.demand.impl;

import com.undercontroll.application.dto.demand.CreateDemandRequest;
import com.undercontroll.application.dto.demand.DemandDto;
import com.undercontroll.domain.usecase.demand.CreateDemandPort;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.model.Demand;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.exception.InvalidDemandException;
import com.undercontroll.domain.gateway.ComponentGateway;
import com.undercontroll.domain.gateway.DemandGateway;
import com.undercontroll.domain.gateway.MetricsGateway;
import com.undercontroll.domain.gateway.OrderGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateDemandImpl implements CreateDemandPort {

    private final DemandGateway demandGateway;
    private final ComponentGateway componentGateway;
    private final OrderGateway orderGateway;
    private final MetricsGateway metricsGateway;

    @Override
    public DemandDto execute(Integer orderId, CreateDemandRequest request) {
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new InvalidDemandException("Quantity should be greater than 0 and not null");
        }

        ComponentPart component = componentGateway.findById(request.componentPartId())
                .orElseThrow(() -> new InvalidDemandException("Component not found"));
        Order order = orderGateway.findById(orderId)
                .orElseThrow(() -> new InvalidDemandException("Order not found"));

        Demand demand = Demand.builder()
                .quantity(request.quantity())
                .component(component)
                .order(order)
                .build();

        log.info("Creating demand for component {} in order {} with quantity {}",
                request.componentPartId(),
                orderId,
                request.quantity());

        Demand savedDemand = demandGateway.save(demand);

        metricsGateway.incrementDemandCreated();

        return new DemandDto(
                savedDemand.getId(),
                savedDemand.getComponent().getId(),
                savedDemand.getOrder().getId(),
                savedDemand.getQuantity()
        );
    }
}
