package com.undercontroll.application.usecase;

import com.undercontroll.domain.port.in.CreateOrderPort;
import com.undercontroll.domain.entity.Order;
import com.undercontroll.domain.entity.ComponentPart;
import com.undercontroll.domain.entity.User;
import com.undercontroll.domain.entity.OrderItem;
import com.undercontroll.domain.entity.enums.OrderStatus;
import com.undercontroll.infrastructure.persistence.repository.OrderJpaRepository;
import com.undercontroll.application.service.OrderItemService;
import com.undercontroll.application.service.DemandService;
import com.undercontroll.application.service.UserService;
import com.undercontroll.application.service.InventoryManagementService;
import com.undercontroll.application.service.MetricsService;
import com.undercontroll.infrastructure.web.dto.PartDto;
import com.undercontroll.infrastructure.web.dto.CreateDemandRequest;
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

    private final OrderJpaRepository repository;
    private final OrderItemService orderItemService;
    private final DemandService demandService;
    private final UserService userService;
    private final InventoryManagementService inventoryManagementService;
    private final MetricsService metricsService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"orders", "ordersByUser", "order", "orderParts", "dashboardMetrics"}, allEntries = true)
    public Output execute(Input input) {
        long startTime = System.currentTimeMillis();
        log.info("Creating new order for user {}", input.userId());

        LocalDate completedFormatted = formatOrderDate(input.deadline());
        LocalDate receivedFormatted = formatOrderDate(input.receivedAt());

        Map<Integer, ComponentPart> validatedComponents = new HashMap<>();
        double partsTotal = 0.0;

        for (PartDto part : input.parts()) {
            ComponentPart component = inventoryManagementService.getComponentById(part.id());
            inventoryManagementService.validateStockAvailability(component, part.quantity());

            validatedComponents.put(part.id(), component);
            partsTotal += part.quantity() * component.getPrice();
        }

        List<OrderItem> orderItems = new ArrayList<>();
        Double laborTotal = 0.0;

        for (var appliance : input.appliances()) {
            Double labor = appliance.laborValue() == null ? 0.0 : appliance.laborValue();

            var orderItemCreated = orderItemService.createOrderItem(
                    new com.undercontroll.infrastructure.web.dto.CreateOrderItemRequest(
                            appliance.brand(),
                            appliance.model(),
                            appliance.type(),
                            "",
                            appliance.customerNote(),
                            appliance.voltage(),
                            appliance.serial(),
                            labor
                    )
            );

            orderItems.add(orderItemCreated);
            laborTotal += orderItemCreated.getLaborValue();
        }

        User user = userService.getUserById(input.userId());
        Double total = partsTotal + laborTotal - input.discount();

        Order order = Order.builder()
                .orderItems(orderItems)
                .status(OrderStatus.PENDING)
                .user(user)
                .discount(input.discount())
                .date(null)
                .store("Loja")
                .nf(input.nf())
                .fabricGuarantee(input.fabricGuarantee())
                .received_at(receivedFormatted)
                .description(input.serviceDescription())
                .returnGuarantee(input.returnGuarantee())
                .completedTime(completedFormatted)
                .total(total)
                .build();

        Order savedOrder = repository.save(order);

        log.info("Order {} created successfully", savedOrder.getId());

        for (PartDto part : input.parts()) {
            ComponentPart component = validatedComponents.get(part.id());

            demandService.createDemand(
                    new CreateDemandRequest(
                            component,
                            Long.valueOf(part.quantity()),
                            savedOrder
                    )
            );

            inventoryManagementService.decreaseStock(component.getId(), part.quantity());
        }

        metricsService.incrementOrderCreated();
        metricsService.recordOrderProcessingTime(startTime);

        log.info("Order {} created with {} demands", savedOrder.getId(), input.parts().size());

        return new Output(
                savedOrder.getId(),
                input.userId(),
                savedOrder.getStatus().toString(),
                savedOrder.getTotal()
        );
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
