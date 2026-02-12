package com.undercontroll.application.usecase;

import com.undercontroll.domain.port.in.CreateDemandPort;
import com.undercontroll.domain.entity.Demand;
import com.undercontroll.domain.exception.InvalidDemandException;
import com.undercontroll.infrastructure.persistence.repository.DemandRepository;
import com.undercontroll.application.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateDemandImpl implements CreateDemandPort {

    private final DemandRepository repository;
    private final MetricsService metricsService;

    @Override
    public Output execute(Input input) {
        if (input.quantity() == null || input.quantity() <= 0) {
            throw new InvalidDemandException("Quantity should be greater than 0 and not null");
        }

        Demand demand = Demand.builder()
                .quantity(input.quantity())
                .component(input.componentPart())
                .order(input.order())
                .build();

        log.info("Creating demand for component {} in order {} with quantity {}",
                input.componentPart().getId(),
                input.order().getId(),
                input.quantity());

        Demand savedDemand = repository.save(demand);

        metricsService.incrementDemandCreated();

        return new Output(
                savedDemand.getId(),
                savedDemand.getComponent().getId(),
                savedDemand.getOrder().getId(),
                savedDemand.getQuantity()
        );
    }
}
