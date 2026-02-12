package com.undercontroll.application.service;

import com.undercontroll.domain.exception.ComponentNotFoundException;
import com.undercontroll.domain.exception.InsuficientComponentException;
import com.undercontroll.domain.entity.ComponentPart;
import com.undercontroll.infrastructure.persistence.repository.ComponentJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;

@Slf4j
@RequiredArgsConstructor
@Service
public class InventoryManagementService {

    private final ComponentJpaRepository componentRepository;
    private final MetricsService metricsService;

    @CacheEvict(value = {"components", "componentsByCategory", "componentsByName", "component"}, allEntries = true)
    public void decreaseStock(Integer componentId, Integer quantity) {
        log.info("Attempting to decrease stock for component {} by {} units", componentId, quantity);

        ComponentPart component = getComponentById(componentId);
        validateStockAvailability(component, quantity);

        Long newQuantity = component.getQuantity() - quantity;
        component.setQuantity(newQuantity);
        componentRepository.save(component);

        metricsService.incrementStockDecreased();

        log.info("Stock decreased successfully for component {}. New quantity: {}",
                componentId, newQuantity);
    }

    @CacheEvict(value = {"components", "componentsByCategory", "componentsByName", "component"}, allEntries = true)
    public void increaseStock(Integer componentId, Integer quantity) {
        log.info("Attempting to increase stock for component {} by {} units", componentId, quantity);

        ComponentPart component = getComponentById(componentId);

        Long newQuantity = component.getQuantity() + quantity;
        component.setQuantity(newQuantity);
        componentRepository.save(component);

        log.info("Stock increased successfully for component {}. New quantity: {}",
                componentId, newQuantity);
    }

    public void validateStockAvailability(ComponentPart component, Integer requiredQuantity) {
        if (component.getQuantity() < requiredQuantity) {
            log.error("Insufficient stock for component {}. Required: {}, Available: {}",
                    component.getId(), requiredQuantity, component.getQuantity());

            metricsService.incrementInsufficientStock(component.getName());

            throw new InsuficientComponentException(
                    String.format("Insufficient stock for component '%s' (ID: %d). Required: %d, Available: %d",
                            component.getName(), component.getId(), requiredQuantity, component.getQuantity()));
        }
    }

    public ComponentPart getComponentById(Integer componentId) {
        return componentRepository.findById(componentId)
                .orElseThrow(() -> {
                    log.error("Component not found with ID: {}", componentId);
                    return new ComponentNotFoundException(
                            "Component not found with id " + componentId);
                });
    }
}
