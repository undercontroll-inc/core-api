package com.undercontroll.application.usecase;

import com.undercontroll.domain.port.in.UpdateComponentPort;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.exception.ComponentNotFoundException;
import com.undercontroll.domain.exception.InvalidUpdateComponentException;
import com.undercontroll.domain.port.out.ComponentRepositoryPort;
import com.undercontroll.domain.port.out.MetricsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateComponentImpl implements UpdateComponentPort {

    private final ComponentRepositoryPort componentRepositoryPort;
    private final MetricsPort metricsPort;

    @Override
    @CacheEvict(value = {"components", "componentsByCategory", "componentsByName", "component"}, allEntries = true)
    public Output execute(Input input) {
        validateUpdate(input.componentId());

        ComponentPart component = componentRepositoryPort.findById(input.componentId())
                .orElseThrow(() -> new ComponentNotFoundException("Component not found with id " + input.componentId()));

        if (input.item() != null && !input.item().isEmpty()) {
            component.setName(input.item());
        }

        if (input.description() != null && !input.description().isEmpty()) {
            component.setDescription(input.description());
        }

        if (input.brand() != null && !input.brand().isEmpty()) {
            component.setBrand(input.brand());
        }

        if (input.category() != null && !input.category().isEmpty()) {
            component.setCategory(input.category());
        }

        if (input.price() != null) {
            component.setPrice(input.price());
        }

        if (input.supplier() != null && !input.supplier().isEmpty()) {
            component.setSupplier(input.supplier());
        }

        ComponentPart savedComponent = componentRepositoryPort.save(component);
        metricsPort.incrementComponentUpdated();

        return new Output(
                savedComponent.getId(),
                savedComponent.getName(),
                savedComponent.getDescription(),
                savedComponent.getBrand(),
                savedComponent.getPrice(),
                savedComponent.getQuantity(),
                savedComponent.getSupplier(),
                savedComponent.getCategory()
        );
    }

    private void validateUpdate(Integer componentId) {
        if (componentId == null || componentId <= 0) {
            throw new InvalidUpdateComponentException("Component id cannot be null or invalid");
        }
    }
}
