package com.undercontroll.application.usecase;

import com.undercontroll.domain.port.in.CreateDemandPort;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.model.Demand;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.exception.InvalidDemandException;
import com.undercontroll.domain.port.out.ComponentRepositoryPort;
import com.undercontroll.domain.port.out.DemandRepositoryPort;
import com.undercontroll.domain.port.out.MetricsPort;
import com.undercontroll.domain.port.out.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateDemandImpl implements CreateDemandPort {

    private final DemandRepositoryPort demandRepositoryPort;
    private final ComponentRepositoryPort componentRepositoryPort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final MetricsPort metricsPort;

    @Override
    public Output execute(Input input) {
        if (input.quantity() == null || input.quantity() <= 0) {
            throw new InvalidDemandException("Quantity should be greater than 0 and not null");
        }

        ComponentPart component = componentRepositoryPort.findById(input.componentPartId())
                .orElseThrow(() -> new InvalidDemandException("Component not found"));
        Order order = orderRepositoryPort.findById(input.orderId())
                .orElseThrow(() -> new InvalidDemandException("Order not found"));

        Demand demand = Demand.builder()
                .quantity(input.quantity())
                .component(component)
                .order(order)
                .build();

        log.info("Creating demand for component {} in order {} with quantity {}",
                input.componentPartId(),
                input.orderId(),
                input.quantity());

        Demand savedDemand = demandRepositoryPort.save(demand);

        metricsPort.incrementDemandCreated();

        return new Output(
                savedDemand.getId(),
                savedDemand.getComponent().getId(),
                savedDemand.getOrder().getId(),
                savedDemand.getQuantity()
        );
    }
}
