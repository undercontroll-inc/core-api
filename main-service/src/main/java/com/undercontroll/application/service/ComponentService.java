package com.undercontroll.application.service;

import com.undercontroll.domain.exception.*;
import com.undercontroll.infrastructure.web.dto.*;
import com.undercontroll.domain.entity.ComponentPart;
import com.undercontroll.infrastructure.persistence.repository.ComponentJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ComponentService {

    private final ComponentJpaRepository repository;
    private final MetricsService metricsService;

    @CacheEvict(value = {"components", "componentsByCategory", "componentsByName", "component"}, allEntries = true)
    public RegisterComponentResponse register(RegisterComponentRequest request) {
        validateCreate(request);

        ComponentPart component = ComponentPart.builder()
                .name(request.item())
                .description(request.description())
                .brand(request.brand())
                .price(request.price())
                .supplier(request.supplier())
                .category(request.category())
                .quantity(request.quantity() != null ? request.quantity() : 0)
                .build();

        repository.save(component);

        metricsService.incrementComponentCreated();

        return new RegisterComponentResponse(
                request.item(),
                request.description(),
                request.brand(),
                request.price(),
                request.supplier(),
                request.category()
        );
    }

    @CacheEvict(value = {"components", "componentsByCategory", "componentsByName", "component"}, allEntries = true)
    public ComponentDto updateComponent(UpdateComponentRequest request, Integer componentId) {
        validateUpdate(componentId);

        ComponentPart component = repository.findById(componentId)
                .orElseThrow(() -> new ComponentNotFoundException("Component not found with id " + componentId));

        if(request.item() != null && !request.item().isEmpty()) {
            component.setName(request.item());
        }

        if(request.description() != null && !request.description().isEmpty()) {
            component.setDescription(request.description());
        }

        if(request.brand() != null && !request.brand().isEmpty()) {
            component.setBrand(request.brand());
        }

        if(request.category() != null && !request.category().isEmpty()) {
            component.setCategory(request.category());
        }

        if(request.price() != null) {
            component.setPrice(request.price());
        }

        if(request.supplier() != null && !request.supplier().isEmpty()) {
            component.setSupplier(request.supplier());
        }

        ComponentDto result = this.mapToDto(repository.save(component));

        metricsService.incrementComponentUpdated();

        return result;
    }

    @Cacheable(value = "components")
    public List<ComponentDto> getComponents() {
        return  repository
                .findAll()
                .stream()
                .map(c -> new ComponentDto(
                        c.getId(),
                        c.getName(),
                        c.getDescription(),
                        c.getBrand(),
                        c.getPrice(),
                        c.getQuantity(),
                        c.getSupplier(),
                        c.getCategory()
                ))
                .toList();
    }

    @Cacheable(value = "componentsByCategory", key = "#category")
    public List<ComponentDto> getComponentsByCategory(String category) {
        if(category == null || category.isEmpty()){
            throw new InvalidGetComponentsByCategoryExcepiton("Category cannot be empty");
        }

        return repository.findByCategory(category)
                .stream()
                .map(c -> new ComponentDto(
                        c.getId(),
                        c.getName(),
                        c.getDescription(),
                        c.getBrand(),
                        c.getPrice(),
                        c.getQuantity(),
                        c.getSupplier(),
                        c.getCategory()
                ))
                .toList();
    }

    @Cacheable(value = "componentsByName", key = "#name")
    public List<ComponentDto> getComponentsByName(String name) {
        if(name == null || name.isEmpty()){
            throw new InvalidGetComponentsByCategoryExcepiton("Name cannot be empty");
        }

        return repository.findByName(name)
                .stream()
                .map(c -> new ComponentDto(
                        c.getId(),
                        c.getName(),
                        c.getDescription(),
                        c.getBrand(),
                        c.getPrice(),
                        c.getQuantity(),
                        c.getSupplier(),
                        c.getCategory()
                ))
                .toList();
    }

    @Cacheable(value = "component", key = "#id")
    public ComponentDto getComponentById(Integer id) {
        return this.mapToDto(repository.findById(id).orElseThrow(
                () -> new ComponentNotFoundException("Could not found the component with id " + id)
        ));
    }

    @Transactional
    @CacheEvict(value = {"components", "componentsByCategory", "componentsByName", "component"}, allEntries = true)
    public void deleteComponent(Integer componentId) {
        validateDelete(componentId);

        ComponentPart component = repository.findById(componentId)
                .orElseThrow(() -> new ComponentNotFoundException("Component not found with id " + componentId));

        // Log para rastreamento
        log.info("Attempting to delete component with id: {}, name: {}", componentId, component.getName());

        // Verificar se há demandas relacionadas
        if (!component.getDemands().isEmpty()) {
            log.warn("Component {} has {} active demand(s) that will be removed",
                    componentId, component.getDemands().size());

            // As demandas serão removidas automaticamente devido ao CascadeType.ALL e orphanRemoval = true
            // Mas vamos logar para auditoria
            component.getDemands().forEach(demand ->
                log.info("Removing demand {} for component {} in order {}",
                        demand.getId(), componentId, demand.getOrder().getId())
            );
        }

        repository.delete(component);
        log.info("Component {} successfully deleted", componentId);
    }

    private void validateCreate(RegisterComponentRequest request) {
        if(
                request.item() == null || request.item().isEmpty() || request.description() == null || request.description().isEmpty() ||
                request.brand() == null || request.brand().isEmpty() || request.price() == null || request.price() <= 0 ||
                request.supplier() == null || request.supplier().isEmpty() || request.category() == null || request.category().isEmpty()
        ) {
            throw new InvalidComponentCreationException("Invalid data for the component creation");
        }
    }

    private void validateUpdate(Integer componentId) {
        if(componentId == null || componentId <= 0) {
            throw new InvalidUpdateComponentException("Component id cannot be null or invalid");
        }
    }

    private void validateDelete(Integer componentId) {
        if(componentId == null || componentId <= 0) {
            throw new InvalidDeleteComponentException("Invalid id for deletion");
        }
    }

    private ComponentDto mapToDto(ComponentPart component) {
        return new ComponentDto(
                component.getId(),
                component.getName(),
                component.getDescription(),
                component.getBrand(),
                component.getPrice(),
                component.getQuantity(),
                component.getSupplier(),
                component.getCategory()
        );
    }

}
