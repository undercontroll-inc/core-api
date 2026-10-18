package com.undercontroll.domain.usecase.component.impl;

import com.undercontroll.domain.usecase.component.RegisterComponentPort;
import com.undercontroll.domain.usecase.component.ComponentOutput;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.exception.InvalidComponentCreationException;
import com.undercontroll.domain.gateway.ComponentGateway;
import com.undercontroll.domain.gateway.MetricsGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterComponentImpl implements RegisterComponentPort {

    private final ComponentGateway componentGateway;
    private final MetricsGateway metricsGateway;

    @Override
    @CacheEvict(value = {"components", "componentsByCategory", "componentsByName", "component"}, allEntries = true)
    public ComponentOutput execute(Input request) {
        validateCreate(request);

        ComponentPart component = ComponentPart.builder()
                .name(request.item())
                .description(request.description())
                .brand(request.brand())
                .price(request.price())
                .supplier(request.supplier())
                .category(request.category())
                .quantity(request.quantity() != null ? request.quantity() : 0L)
                .build();

        ComponentPart savedComponent = componentGateway.save(component);
        metricsGateway.incrementComponentCreated();
        log.info("Component created id={} name={}", savedComponent.getId(), savedComponent.getName());
        return ComponentOutput.from(savedComponent);
    }

    private void validateCreate(Input request) {
        if (request.item() == null || request.item().isEmpty() ||
            request.description() == null || request.description().isEmpty() ||
            request.brand() == null || request.brand().isEmpty() ||
            request.price() == null || request.price() <= 0 ||
            request.supplier() == null || request.supplier().isEmpty() ||
            request.category() == null || request.category().isEmpty()) {
            throw new InvalidComponentCreationException("Invalid data for the component creation");
        }
    }
}
