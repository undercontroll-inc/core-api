package com.undercontroll.domain.usecase.component.impl;

import com.undercontroll.application.dto.component.ComponentDto;
import com.undercontroll.application.dto.component.UpdateComponentRequest;
import com.undercontroll.application.mapper.ComponentPartDtoMapper;
import com.undercontroll.domain.usecase.component.UpdateComponentPort;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.exception.ComponentNotFoundException;
import com.undercontroll.domain.exception.InvalidUpdateComponentException;
import com.undercontroll.domain.gateway.ComponentGateway;
import com.undercontroll.domain.gateway.MetricsGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateComponentImpl implements UpdateComponentPort {

    private final ComponentGateway componentGateway;
    private final MetricsGateway metricsGateway;
    private final ComponentPartDtoMapper componentPartDtoMapper;

    @Override
    @CacheEvict(value = {"components", "componentsByCategory", "componentsByName", "component"}, allEntries = true)
    public ComponentDto execute(Integer componentId, UpdateComponentRequest request) {
        validateUpdate(componentId);

        ComponentPart component = componentGateway.findById(componentId)
                .orElseThrow(() -> new ComponentNotFoundException("Component not found with id " + componentId));

        if (request.item() != null && !request.item().isEmpty()) {
            component.setName(request.item());
        }

        if (request.description() != null && !request.description().isEmpty()) {
            component.setDescription(request.description());
        }

        if (request.brand() != null && !request.brand().isEmpty()) {
            component.setBrand(request.brand());
        }

        if (request.category() != null && !request.category().isEmpty()) {
            component.setCategory(request.category());
        }

        if (request.price() != null) {
            component.setPrice(request.price());
        }

        if (request.supplier() != null && !request.supplier().isEmpty()) {
            component.setSupplier(request.supplier());
        }

        ComponentPart savedComponent = componentGateway.save(component);
        metricsGateway.incrementComponentUpdated();
        log.info("Component updated id={}", savedComponent.getId());
        return componentPartDtoMapper.toDto(savedComponent);
    }

    private void validateUpdate(Integer componentId) {
        if (componentId == null || componentId <= 0) {
            throw new InvalidUpdateComponentException("Component id cannot be null or invalid");
        }
    }
}
