package com.undercontroll.application.usecase.component.impl;

import com.undercontroll.application.usecase.component.DeleteComponentPort;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.exception.ComponentNotFoundException;
import com.undercontroll.domain.exception.InvalidDeleteComponentException;
import com.undercontroll.domain.repository.ComponentRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteComponentImpl implements DeleteComponentPort {

    private final ComponentRepositoryPort componentRepositoryPort;

    @Override
    @Transactional
    @CacheEvict(value = {"components", "componentsByCategory", "componentsByName", "component"}, allEntries = true)
    public Output execute(Input input) {
        validateDelete(input.componentId());

        ComponentPart component = componentRepositoryPort.findById(input.componentId())
                .orElseThrow(() -> new ComponentNotFoundException("Component not found with id " + input.componentId()));

        log.info("Attempting to delete component with id: {}, name: {}", input.componentId(), component.getName());

        if (!component.getDemands().isEmpty()) {
            log.warn("Component {} has {} active demand(s) that will be removed",
                    input.componentId(), component.getDemands().size());

            component.getDemands().forEach(demand ->
                log.info("Removing demand {} for component {} in order {}",
                        demand.getId(), input.componentId(), demand.getOrder().getId())
            );
        }

        componentRepositoryPort.deleteById(component.getId());
        log.info("Component {} successfully deleted", input.componentId());

        return new Output(true, "Component deleted successfully");
    }

    private void validateDelete(Integer componentId) {
        if (componentId == null || componentId <= 0) {
            throw new InvalidDeleteComponentException("Invalid id for deletion");
        }
    }
}
