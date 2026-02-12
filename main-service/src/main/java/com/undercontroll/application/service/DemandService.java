package com.undercontroll.application.service;

import com.undercontroll.infrastructure.web.dto.CreateDemandRequest;
import com.undercontroll.infrastructure.web.dto.DemandDto;
import com.undercontroll.domain.exception.InvalidDemandException;
import com.undercontroll.domain.entity.Demand;
import com.undercontroll.domain.entity.Order;
import com.undercontroll.infrastructure.persistence.repository.ComponentJpaRepository;
import com.undercontroll.infrastructure.persistence.repository.DemandRepository;
import com.undercontroll.infrastructure.persistence.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class DemandService {

    private final DemandRepository repository;
    private final MetricsService metricsService;
    private final ComponentJpaRepository componentRepository;
    private final OrderJpaRepository orderRepository;

    public Demand createDemand(CreateDemandRequest createDemandRequest) {
        if(createDemandRequest.quantity() == null || createDemandRequest.quantity() <= 0) {
            throw new InvalidDemandException("Quantity should be greater than 0 and not null");
        }

        Demand demand = Demand.builder()
                .quantity(createDemandRequest.quantity())
                .component(createDemandRequest.componentPart())
                .order(createDemandRequest.order())
                .build();

        log.info("Creating demand for component {} in order {} with quantity {}",
                createDemandRequest.componentPart().getId(),
                createDemandRequest.order().getId(),
                createDemandRequest.quantity());

        Demand savedDemand = repository.save(demand);

        metricsService.incrementDemandCreated();

        return savedDemand;
    }

    public Demand updateDemand(Demand demand) {
        log.info("Updating demand {} for component {} with new quantity {}",
                demand.getId(),
                demand.getComponent().getId(),
                demand.getQuantity());
        return repository.save(demand);
    }

    public List<Demand> findDemandsByOrder(Order order) {
        return repository.findByOrder(order);
    }

    public Optional<Demand> findDemandByOrderAndComponent(Order order, Integer componentId) {
        return repository.findByOrderAndComponent_Id(order, componentId);
    }

    public void deleteDemand(Demand demand) {
        log.info("Deleting demand {} for component {} in order {}",
                demand.getId(),
                demand.getComponent().getId(),
                demand.getOrder().getId());

        repository.delete(demand);

        metricsService.incrementDemandRemoved();
    }

    public void deleteAllDemandsForOrder(Order order) {
        log.info("Deleting all demands for order {}", order.getId());
        repository.deleteByOrder(order);
    }


    public List<DemandDto> getDemandsByOrderId(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new InvalidDemandException("Order not found with id: " + orderId));

        return findDemandsByOrder(order).stream()
                .map(this::mapToDto)
                .toList();
    }

    public DemandDto getDemandByOrderAndComponentId(Integer orderId, Integer componentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new InvalidDemandException("Order not found with id: " + orderId));

        Demand demand = findDemandByOrderAndComponent(order, componentId)
                .orElseThrow(() -> new InvalidDemandException(
                        "Demand not found for component " + componentId + " in order " + orderId));

        return mapToDto(demand);
    }

    public void deleteDemandById(Integer demandId) {
        Demand demand = repository.findById(demandId)
                .orElseThrow(() -> new InvalidDemandException("Demand not found with id: " + demandId));

        deleteDemand(demand);
    }

    public void deleteAllDemandsByOrderId(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new InvalidDemandException("Order not found with id: " + orderId));

        deleteAllDemandsForOrder(order);
    }

    private DemandDto mapToDto(Demand demand) {
        return new DemandDto(
                demand.getId(),
                demand.getComponent().getId(),
                demand.getOrder().getId(),
                demand.getQuantity()
        );
    }

}
