package com.undercontroll.domain.usecase.order.impl;

import com.undercontroll.application.dto.order.CreateOrderRequest;
import com.undercontroll.application.dto.demand.CreateDemandRequest;
import com.undercontroll.application.dto.order.OrderEnrichedDto;
import com.undercontroll.application.mapper.OrderDtoMapper;
import com.undercontroll.domain.usecase.order.CreateOrderPort;
import com.undercontroll.domain.usecase.demand.CreateDemandPort;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.OrderItem;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.model.User;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.gateway.UserGateway;
import com.undercontroll.domain.gateway.StockManagementGateway;
import com.undercontroll.domain.gateway.CurrentUserAdminPort;
import com.undercontroll.infrastructure.service.MetricsService;
import com.undercontroll.application.dto.order.PartDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateOrderImpl implements CreateOrderPort {

    private final OrderGateway orderGateway;
    private final UserGateway userGateway;
    private final StockManagementGateway stockManagementGateway;
    private final CreateDemandPort createDemandPort;
    private final MetricsService metricsService;
    private final OrderDtoMapper orderDtoMapper;
    private final CurrentUserAdminPort currentUserAdminPort;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"orders", "ordersByUser", "order", "orderParts", "dashboardMetrics"}, allEntries = true)
    public OrderEnrichedDto execute(CreateOrderRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Creating new order for user {}", request.userId());

        LocalDate completedFormatted = formatOrderDate(request.deadline());
        LocalDate receivedFormatted = formatOrderDate(request.receivedAt());

        Map<Integer, ComponentPart> validatedComponents = new HashMap<>();
        double partsTotal = 0.0;

        for (PartDto part : request.parts()) {
            ComponentPart component = stockManagementGateway.findComponentById(part.componentId())
                    .orElseThrow(() -> new RuntimeException("Component not found"));
            stockManagementGateway.validateStockAvailability(component, part.quantity());

            validatedComponents.put(part.componentId(), component);
            partsTotal += part.quantity() * component.getPrice();
        }

        List<OrderItem> orderItems = new ArrayList<>();
        Double laborTotal = 0.0;

        for (var appliance : request.appliances()) {
            Double labor = appliance.laborValue() == null ? 0.0 : appliance.laborValue();
            orderItems.add(OrderItem.builder()
                    .brand(appliance.brand())
                    .model(appliance.model())
                    .type(appliance.type())
                    .volt(appliance.voltage())
                    .series(appliance.serial())
                    .laborValue(labor)
                    .build());
            laborTotal += labor;
        }

        User user = userGateway.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Double total = partsTotal + laborTotal - request.discount();

        Order order = Order.builder()
                .orderItems(orderItems)
                .status(OrderStatus.PENDING)
                .user(user)
                .discount(request.discount())
                .date(null)
                .store("Loja")
                .nf(request.nf())
                .fabricGuarantee(request.fabricGuarantee())
                .received_at(receivedFormatted)
                .customerDescription(request.customerDescription())
                .technicalDescription(currentUserAdminPort.isAdministrator() ? request.technicalDescription() : null)
                .returnGuarantee(request.returnGuarantee())
                .completedTime(completedFormatted)
                .total(total)
                .build();

        Order savedOrder = orderGateway.save(order);

        log.info("Order {} created successfully", savedOrder.getId());

        for (PartDto part : request.parts()) {
            ComponentPart component = validatedComponents.get(part.componentId());

            createDemandPort.execute(savedOrder.getId(), new CreateDemandRequest(
                    component.getId(),
                    Long.valueOf(part.quantity())
            ));

            stockManagementGateway.decreaseStock(component.getId(), part.quantity());
        }

        metricsService.incrementOrderCreated();
        metricsService.recordOrderProcessingTime(startTime);

        log.info("Order {} created with {} demands", savedOrder.getId(), request.parts().size());

        Order finalOrder = orderGateway.findById(savedOrder.getId()).orElse(savedOrder);
        return orderDtoMapper.toEnrichedDto(finalOrder, currentUserAdminPort.isAdministrator());
    }

    private LocalDate formatOrderDate(String dateStr) {
        if (dateStr == null) return null;
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return LocalDate.parse(dateStr);
        }
    }
}
