package com.undercontroll.infrastructure.persistence.adapter;

import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.exception.ComponentNotFoundException;
import com.undercontroll.domain.exception.InsuficientComponentException;
import com.undercontroll.domain.port.out.StockManagementPort;
import com.undercontroll.infrastructure.persistence.entity.ComponentPartJpaEntity;
import com.undercontroll.infrastructure.persistence.repository.ComponentJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class StockManagementAdapter implements StockManagementPort {

    private final ComponentJpaRepository componentJpaRepository;

    @Override
    public Optional<ComponentPart> findComponentById(Integer componentId) {
        return componentJpaRepository.findById(componentId)
                .map(ComponentPartJpaEntity::toDomain);
    }

    @Override
    public ComponentPart save(ComponentPart component) {
        ComponentPartJpaEntity jpaEntity = ComponentPartJpaEntity.fromDomain(component);
        ComponentPartJpaEntity savedEntity = componentJpaRepository.save(jpaEntity);
        return savedEntity.toDomain();
    }

    @Override
    @CacheEvict(value = {"components", "componentsByCategory", "componentsByName", "component"}, allEntries = true)
    public void decreaseStock(Integer componentId, Integer quantity) {
        log.info("Attempting to decrease stock for component {} by {} units", componentId, quantity);

        ComponentPart component = findComponentById(componentId)
                .orElseThrow(() -> {
                    log.error("Component not found with ID: {}", componentId);
                    return new ComponentNotFoundException("Component not found with id " + componentId);
                });

        validateStockAvailability(component, quantity);

        Long newQuantity = component.getQuantity() - quantity;
        component.setQuantity(newQuantity);
        save(component);

        log.info("Stock decreased successfully for component {}. New quantity: {}",
                componentId, newQuantity);
    }

    @Override
    @CacheEvict(value = {"components", "componentsByCategory", "componentsByName", "component"}, allEntries = true)
    public void increaseStock(Integer componentId, Integer quantity) {
        log.info("Attempting to increase stock for component {} by {} units", componentId, quantity);

        ComponentPart component = findComponentById(componentId)
                .orElseThrow(() -> {
                    log.error("Component not found with ID: {}", componentId);
                    return new ComponentNotFoundException("Component not found with id " + componentId);
                });

        Long newQuantity = component.getQuantity() + quantity;
        component.setQuantity(newQuantity);
        save(component);

        log.info("Stock increased successfully for component {}. New quantity: {}",
                componentId, newQuantity);
    }

    @Override
    public void validateStockAvailability(ComponentPart component, Integer requiredQuantity) {
        if (component.getQuantity() < requiredQuantity) {
            log.error("Insufficient stock for component {}. Required: {}, Available: {}",
                    component.getId(), requiredQuantity, component.getQuantity());

            throw new InsuficientComponentException(
                    String.format("Insufficient stock for component '%s' (ID: %d). Required: %d, Available: %d",
                            component.getName(), component.getId(), requiredQuantity, component.getQuantity()));
        }
    }
}
