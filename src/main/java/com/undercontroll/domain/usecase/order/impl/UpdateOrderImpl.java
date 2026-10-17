package com.undercontroll.domain.usecase.order.impl;

import com.undercontroll.application.dto.demand.CreateDemandRequest;
import com.undercontroll.application.dto.order.PartDto;
import com.undercontroll.application.dto.order.UpdateOrderRequest;
import com.undercontroll.application.dto.orderitem.UpdateOrderItemDto;
import com.undercontroll.application.dto.orderitem.UpdateOrderItemRequest;
import com.undercontroll.domain.model.Demand;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.OrderItem;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.exception.OrderNotFoundException;
import com.undercontroll.domain.exception.InvalidUpdateOrderException;
import com.undercontroll.domain.gateway.DemandGateway;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.usecase.demand.CreateDemandPort;
import com.undercontroll.domain.usecase.order.UpdateOrderPort;
import com.undercontroll.domain.usecase.order_item.UpdateOrderItemPort;
import com.undercontroll.domain.gateway.MetricsGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateOrderImpl implements UpdateOrderPort {

    private final OrderGateway orderGateway;
    private final MetricsGateway metricsGateway;
    private final UpdateOrderItemPort updateOrderItemPort;
    private final DemandGateway demandGateway;
    private final CreateDemandPort createDemandPort;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"orders", "ordersByUser", "order", "orderParts", "dashboardMetrics"}, allEntries = true)
    public void execute(Integer orderId, UpdateOrderRequest request) {
        try {
            log.info("Updating order {}", orderId);

            validateUpdateOrder(orderId);

            Order order = orderGateway.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException("Could not found the order while updating."));

            applyStatus(order, orderId, request.status());
            if (request.customerDescription() != null) {
                order.setCustomerDescription(request.customerDescription());
            }
            if (request.technicalDescription() != null) {
                order.setTechnicalDescription(request.technicalDescription());
            }
            applyAppliances(order, orderId, request.appliances());
            applyParts(order, request.parts());

            orderGateway.save(order);
            log.info("Order {} updated successfully", orderId);
        } catch (Exception e) {
            metricsGateway.incrementOrderUpdateFailed();
            throw e;
        }
    }

    private void applyStatus(Order order, Integer orderId, OrderStatus status) {
        if (status == null) {
            return;
        }
        order.setStatus(status);
        log.info("Order {} status updated to {}", orderId, status);
        if (status == OrderStatus.COMPLETED) {
            metricsGateway.incrementOrderCompleted();
        }
    }

    private void applyAppliances(Order order, Integer orderId, List<UpdateOrderItemDto> appliances) {
        if (appliances == null) {
            return;
        }
        for (UpdateOrderItemDto dto : appliances) {
            if (dto.id() != null) {
                updateOrderItemPort.execute(orderId, dto.id(), new UpdateOrderItemRequest(
                        null,
                        dto.laborValue(),
                        dto.volt(),
                        dto.series(),
                        dto.type(),
                        dto.brand(),
                        dto.model(),
                        null
                ));
            } else {
                order.addOrderItem(OrderItem.builder()
                        .type(dto.type())
                        .brand(dto.brand())
                        .model(dto.model())
                        .volt(dto.volt())
                        .series(dto.series())
                        .laborValue(dto.laborValue() != null ? dto.laborValue() : 0.0)
                        .build());
            }
        }
    }

    private void applyParts(Order order, List<PartDto> parts) {
        if (parts == null) {
            return;
        }
        for (PartDto part : parts) {
            if (part.componentId() == null) {
                continue;
            }
            Optional<Demand> existing = demandGateway.findByOrderAndComponentId(order, part.componentId());
            boolean keep = part.quantity() != null && part.quantity() > 0;
            if (existing.isPresent() && keep) {
                Demand demand = existing.get();
                demand.setQuantity(part.quantity().longValue());
                demandGateway.save(demand);
            } else if (existing.isPresent()) {
                demandGateway.deleteById(existing.get().getId());
            } else if (keep) {
                createDemandPort.execute(order.getId(), new CreateDemandRequest(
                        part.componentId(),
                        part.quantity().longValue()
                ));
            }
        }
    }

    private void validateUpdateOrder(Integer id) {
        if (id == null || id <= 0) {
            throw new InvalidUpdateOrderException("Order id cannot be null for the update");
        }
    }
}
