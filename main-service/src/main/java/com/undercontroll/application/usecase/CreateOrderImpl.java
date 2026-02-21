package com.undercontroll.application.usecase;

import com.undercontroll.domain.port.in.CreateOrderPort;
import com.undercontroll.domain.port.in.CreateOrderItemPort;
import com.undercontroll.domain.port.in.CreateDemandPort;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.model.User;
import com.undercontroll.domain.model.enums.OrderStatus;
import com.undercontroll.domain.port.out.OrderRepositoryPort;
import com.undercontroll.domain.port.out.UserRepositoryPort;
import com.undercontroll.domain.port.out.StockManagementPort;
import com.undercontroll.domain.port.out.MetricsPort;
import com.undercontroll.infrastructure.web.dto.PartDto;
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

    private final OrderRepositoryPort orderRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final StockManagementPort stockManagementPort;
    private final CreateOrderItemPort createOrderItemPort;
    private final CreateDemandPort createDemandPort;
    private final MetricsPort metricsPort;

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
            ComponentPart component = stockManagementPort.findComponentById(part.id())
                    .orElseThrow(() -> new RuntimeException("Component not found"));
            stockManagementPort.validateStockAvailability(component, part.quantity());

            validatedComponents.put(part.id(), component);
            partsTotal += part.quantity() * component.getPrice();
        }

        List<CreateOrderItemPort.Output> orderItems = new ArrayList<>();
        Double laborTotal = 0.0;

        for (var appliance : input.appliances()) {
            Double labor = appliance.laborValue() == null ? 0.0 : appliance.laborValue();

            var orderItemCreated = createOrderItemPort.execute(new CreateOrderItemPort.Input(
                    appliance.brand(),
                    appliance.model(),
                    appliance.type(),
                    "",
                    appliance.customerNote(),
                    appliance.voltage(),
                    appliance.serial(),
                    labor
            ));

            orderItems.add(orderItemCreated);
            laborTotal += orderItemCreated.laborValue();
        }

        User user = userRepositoryPort.findById(input.userId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
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

        Order savedOrder = orderRepositoryPort.save(order);

        log.info("Order {} created successfully", savedOrder.getId());

        for (PartDto part : input.parts()) {
            ComponentPart component = validatedComponents.get(part.id());

            createDemandPort.execute(new CreateDemandPort.Input(
                    component,
                    Long.valueOf(part.quantity()),
                    savedOrder
            ));

            stockManagementPort.decreaseStock(component.getId(), part.quantity());
        }

        metricsPort.incrementOrderCreated();
        metricsPort.recordOrderProcessingTime(startTime);

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
