package com.undercontroll.application.usecase;

import com.undercontroll.domain.port.in.RegisterComponentPort;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.exception.InvalidComponentCreationException;
import com.undercontroll.domain.port.out.ComponentRepositoryPort;
import com.undercontroll.domain.port.out.MetricsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterComponentImpl implements RegisterComponentPort {

    private final ComponentRepositoryPort componentRepositoryPort;
    private final MetricsPort metricsPort;

    @Override
    @CacheEvict(value = {"components", "componentsByCategory", "componentsByName", "component"}, allEntries = true)
    public Output execute(Input input) {
        validateCreate(input);

        ComponentPart component = ComponentPart.builder()
                .name(input.item())
                .description(input.description())
                .brand(input.brand())
                .price(input.price())
                .supplier(input.supplier())
                .category(input.category())
                .quantity(input.quantity() != null ? input.quantity() : 0L)
                .build();

        componentRepositoryPort.save(component);
        metricsPort.incrementComponentCreated();

        return new Output(
                input.item(),
                input.description(),
                input.brand(),
                input.price(),
                input.supplier(),
                input.category()
        );
    }

    private void validateCreate(Input input) {
        if (input.item() == null || input.item().isEmpty() ||
            input.description() == null || input.description().isEmpty() ||
            input.brand() == null || input.brand().isEmpty() ||
            input.price() == null || input.price() <= 0 ||
            input.supplier() == null || input.supplier().isEmpty() ||
            input.category() == null || input.category().isEmpty()) {
            throw new InvalidComponentCreationException("Invalid data for the component creation");
        }
    }
}
