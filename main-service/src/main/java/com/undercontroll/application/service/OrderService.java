package com.undercontroll.application.service;

import com.undercontroll.domain.exception.*;
import com.undercontroll.infrastructure.web.dto.*;
import com.undercontroll.domain.entity.*;
import com.undercontroll.domain.entity.enums.OrderStatus;
import com.undercontroll.domain.entity.enums.UserType;
import com.undercontroll.infrastructure.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderJpaRepository repository;
    private final OrderItemService orderItemService;
    private final DemandService demandService;
    private final UserService userService;
    private final InventoryManagementService inventoryManagementService;
    private final MetricsService metricsService;
    private final PdfExportService pdfExportService;

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"orders", "ordersByUser", "order", "orderParts", "dashboardMetrics"}, allEntries = true)
    public Order createOrder(CreateOrderRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Creating new order for user {}", request.userId());

        LocalDate completedFormatted = this.formatOrderDate(request.deadline());
        LocalDate receivedFormatted = this.formatOrderDate(request.receivedAt());

        // Hash map utilizado para validar a presença da quantidade necessária de componentes no estoque
        // e para calcular o valor total dos componentes.
        Map<Integer, ComponentPart> validatedComponents = new HashMap<>();
        double partsTotal = 0.0;

        for (PartDto part : request.parts()) {
            ComponentPart component = inventoryManagementService.getComponentById(part.id());
            inventoryManagementService.validateStockAvailability(component, part.quantity());

            validatedComponents.put(part.id(), component);
            partsTotal += part.quantity() * component.getPrice();
        }

        List<OrderItem> orderItems = new ArrayList<>();
        Double laborTotal = 0.0;

        for (OrderItemCreateOrderRequest appliance : request.appliances()) {

            // Pode ser que ele não defina um valor de mão de obra durante a criação da OS
            Double labor = appliance.laborValue() == null ? 0.0 : appliance.laborValue();

            OrderItem orderItemCreated = orderItemService.createOrderItem(
                    new CreateOrderItemRequest(
                            appliance.brand(),
                            appliance.model(),
                            appliance.type(),
                            "", // Future: image URL
                            appliance.customerNote(),
                            appliance.voltage(),
                            appliance.serial(),
                            labor
                    )
            );

            orderItems.add(orderItemCreated);
            laborTotal += orderItemCreated.getLaborValue();
        }

        User user = userService.getUserById(request.userId());
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
                .description(request.serviceDescription())
                .returnGuarantee(request.returnGuarantee())
                .completedTime(completedFormatted)
                .total(total)
                .build();

        Order savedOrder = repository.save(order);

        log.info("Order {} created successfully", savedOrder.getId());

        for (PartDto part : request.parts()) {
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

        log.info("Order {} created with {} demands", savedOrder.getId(), request.parts().size());
        return savedOrder;
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"orders", "ordersByUser", "order", "orderParts", "dashboardMetrics"}, allEntries = true)
    public void updateOrder(UpdateOrderRequest request, Integer id) {
        try {
            log.info("Updating order {}", id);

            validateUpdateOrder(request, id);

            Order order = repository.findById(id)
                    .orElseThrow(() -> new OrderNotFoundException("Could not found the order while updating."));

            updateDemands(request.parts(), order);

            updateOrderItems(request, order);

            OrderStatus previousStatus = order.getStatus();

            if (request.status() != null) {
                order.setStatus(request.status());
                log.info("Order {} status updated to {}", id, request.status());

                if (request.status() == OrderStatus.COMPLETED && previousStatus != OrderStatus.COMPLETED) {
                    metricsService.incrementOrderCompleted();
                }
            }

            if (request.serviceDescription() != null) {
                order.setDescription(request.serviceDescription());
            }

            repository.save(order);
            log.info("Order {} updated successfully", id);
        } catch (Exception e) {
            metricsService.incrementOrderUpdateFailed();
            throw e;
        }
    }

    private void updateOrderItems(UpdateOrderRequest request, Order order) {
        // Se não vierem itens para atualizar, não faz nada
        if (request.appliances() == null || request.appliances().isEmpty()) {
            log.info("No order items to update for order {}", order.getId());
            return;
        }

        for (UpdateOrderItemDto appliance : request.appliances()) {
            if (appliance.id() == null) {
                OrderItem orderItemCreated = orderItemService.createOrderItem(
                        new CreateOrderItemRequest(
                                appliance.brand(),
                                appliance.model(),
                                appliance.type(),
                                "", // Future: lidar com imagens
                                appliance.customerNote(),
                                appliance.volt(),
                                appliance.series(),
                                appliance.laborValue()
                        )
                );

                order.addOrderItem(orderItemCreated);
                log.info("Added new order item to order {}", order.getId());
            } else {
                orderItemService.updateOrderItem(new UpdateOrderItemRequest(
                        appliance.id(),
                        "", // Future: lidar com imagens
                        appliance.laborValue(),
                        appliance.customerNote(),
                        appliance.volt(),
                        appliance.series(),
                        appliance.type(),
                        appliance.brand(),
                        appliance.model(),
                        request.status() == OrderStatus.COMPLETED ? LocalDateTime.now() : null
                ));
                log.info("Updated order item {} in order {}", appliance.id(), order.getId());
            }
        }
    }


    private void updateDemands(List<PartDto> newParts, Order order) {
        log.info("Updating demands for order {}", order.getId());

        if (newParts == null || newParts.isEmpty()) {
            log.info("No demand changes for order {}", order.getId());
            return;
        }

        for (PartDto part : newParts) {
            if (part.id() == null) {
                throw new ComponentNotFoundException("Component ID cannot be null when updating demands");
            }

            ComponentPart component = inventoryManagementService.getComponentById(part.id());

            Demand existingDemand = demandService.findDemandByOrderAndComponent(order, part.id())
                    .orElseThrow(() -> new InvalidDemandException(
                            String.format("No demand found for component %d in order %d", part.id(), order.getId())));

            Long oldQuantity = existingDemand.getQuantity();
            Integer newQuantity = part.quantity();

            // Se a quantidade for zero, remove a demanda e restaura o estoque
            if (newQuantity == 0) {
                log.info("Removing demand for component {} from order {} (quantity set to 0)",
                        part.id(), order.getId());

                // Restaura o estoque completo da quantidade antiga
                inventoryManagementService.increaseStock(part.id(), oldQuantity.intValue());

                // Remove a demanda do pedido
                demandService.deleteDemand(existingDemand);

                log.info("Demand removed and stock restored for component {}", part.id());
                continue;
            }

            if (!oldQuantity.equals(Long.valueOf(newQuantity))) {
                log.info("Updating demand for component {} in order {}. Old quantity: {}, New quantity: {}",
                        part.id(), order.getId(), oldQuantity, newQuantity);

                int quantityDelta = newQuantity - oldQuantity.intValue();

                if (quantityDelta > 0) {
                    // Precisa de mais componentes - valida estoque e diminui
                    inventoryManagementService.validateStockAvailability(component, quantityDelta);
                    inventoryManagementService.decreaseStock(part.id(), quantityDelta);
                } else {
                    // Precisa de menos componentes - devolve ao estoque
                    inventoryManagementService.increaseStock(part.id(), Math.abs(quantityDelta));
                }

                existingDemand.setQuantity(Long.valueOf(newQuantity));
                demandService.updateDemand(existingDemand);
            } else {
                log.info("Quantity unchanged for component {} in order {}", part.id(), order.getId());
            }
        }

        log.info("Demands updated successfully for order {}", order.getId());
    }

    @Cacheable(value = "orders")
    public GetAllOrdersResponse getOrders() {
        log.info("Fetching all orders");
        List<Order> orders = repository.findAll();
        List<OrderEnrichedDto> enrichedOrders = mapOrdersToEnrichedDtos(orders);
        return new GetAllOrdersResponse(enrichedOrders);
    }

    @Cacheable(value = "order", key = "#orderId")
    public GetOrderByIdResponse getOrderById(Integer orderId, String email) {
        log.info("Fetching order with id {}", orderId);

        if (orderId == null || orderId <= 0) {
            throw new InvalidUpdateOrderException("Order id must be a positive number");
        }

        Order order = repository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        String.format("Order with id %d not found", orderId)));

        User user = userService.getUserByEmail(email);

        validateOrderUser(order, user);

        OrderEnrichedDto enrichedOrder = mapOrderToEnrichedDto(order);
        return new GetOrderByIdResponse(enrichedOrder);
    }

    @Cacheable(value = "ordersByUser", key = "#userId")
    public GetOrdersByUserIdResponse getOrdersByUserId(Integer userId) {
        log.info("Fetching orders for user id {}", userId);

        List<Order> orders = repository.findByUser_id(userId);

        List<OrderEnrichedDto> enrichedOrders = mapOrdersToEnrichedDtos(orders);
        return new GetOrdersByUserIdResponse(enrichedOrders);
    }

    @CacheEvict(value = {"orders", "ordersByUser", "order", "orderParts", "dashboardMetrics"}, allEntries = true)
    public void deleteOrder(Integer orderId) {
        log.info("Deleting order with id {}", orderId);
        validateDeleteOrder(orderId);

        Order order = repository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Could not found the order"));

        repository.delete(order);
        log.info("Order {} deleted successfully", orderId);
    }

    @Cacheable(value = "orderParts", key = "#orderId")
    public List<ComponentDto> getPartsByOrderId(Integer orderId) {
        List<Object[]> results = repository.findAllPartsByOrderIdNative(orderId);

        return results.stream()
                .map(row -> new ComponentDto(
                        (Integer) row[0],  // componentId
                        (String) row[1],   // name
                        (String) row[2],   // description
                        (String) row[3],   // brand
                        (Double) row[4],   // price
                        (Long) row[7],     // demandQuantity
                        (String) row[5],   // supplier
                        (String) row[6]   // category
                ))
                .toList();
    }

    public byte[] exportPdf(Integer orderId) {
        log.info("Exporting order {} to PDF", orderId);

        Order order = repository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        String.format("Order with id %d not found", orderId)));

        OrderEnrichedDto enrichedOrder = mapOrderToEnrichedDto(order);

        List<ExportOrderRequest.ProductInfo> produtos = enrichedOrder.appliances().stream()
                .map(item -> new ExportOrderRequest.ProductInfo(
                        item.type() + " " + item.brand() + " " + item.model(),
                        item.volt(),
                        item.series()
                ))
                .toList();

        List<ExportOrderRequest.PartInfo> pecas = enrichedOrder.parts().stream()
                .map(part -> new ExportOrderRequest.PartInfo(
                        part.quantity(),
                        part.item(),
                        String.format("R$ %.2f", part.price() * part.quantity())
                ))
                .toList();

        ExportOrderRequest exportRequest = new ExportOrderRequest(
                String.valueOf(enrichedOrder.id()),
                String.valueOf(enrichedOrder.id()),
                enrichedOrder.nf(),
                enrichedOrder.receivedAt(),
                "Loja",
                produtos,
                enrichedOrder.user().name(),
                enrichedOrder.user().address(),
                enrichedOrder.user().phone(),
                enrichedOrder.receivedAt(),
                pecas,
                String.format("R$ %.2f", enrichedOrder.totalValue()),
                enrichedOrder.deadline(),
                "Técnico",
                order.isFabricGuarantee(),
                false, // orcamento - você pode adicionar este campo ao Order se necessário
                order.isReturnGuarantee()
        );

        return pdfExportService.exportOS(exportRequest);
    }

    private List<OrderEnrichedDto> mapOrdersToEnrichedDtos(List<Order> orders) {
        return orders.stream()
                .map(this::mapOrderToEnrichedDto)
                .toList();
    }

    private OrderEnrichedDto mapOrderToEnrichedDto(Order order) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");


        Double partsTotal = repository.calculatePartsTotalByOrderId(order.getId());
        Double laborTotal = calculateLaborTotal(order);
        List<OrderItemDto> orderItemDtos = mapOrderItems(order);
        List<ComponentDto> parts = getPartsByOrderId(order.getId());
        Double totalValue = partsTotal + laborTotal - order.getDiscount();

        return new OrderEnrichedDto(
                order.getId(),
                userService.mapToDto(order.getUser()),
                orderItemDtos,
                parts,
                partsTotal,
                laborTotal,
                order.getDiscount(),
                totalValue,
                formatDate(order.getReceived_at(), formatter),
                formatDate(order.getCompletedTime(), formatter),
                order.getNf(),
                order.isReturnGuarantee(),
                order.getDescription(),
                null,
                order.getStatus(),
                order.getUpdatedAt().format(formatter)
        );
    }

    private Double calculateLaborTotal(Order order) {
        return order.getOrderItems().stream()
                .mapToDouble(OrderItem::getLaborValue)
                .sum();
    }

    private List<OrderItemDto> mapOrderItems(Order order) {
        return order.getOrderItems().stream()
                .map(orderItemService::mapToDto)
                .toList();
    }

    private String formatDate(LocalDate date, DateTimeFormatter formatter) {
        return date == null ? null : date.format(formatter);
    }

    private void validateUpdateOrder(UpdateOrderRequest request, Integer id) {
        if(id == null || id <= 0){
            throw new InvalidUpdateOrderException("Order id cannot be null for the update");
        }
    }

    private void validateOrderUser(Order order, User user) {
        log.info("Usuário autenticado: {}", user);

        if(!user.getId().equals(order.getUser().getId()) && !user.getUserType().equals(UserType.ADMINISTRATOR)) {
            throw new UnauthorizedOrderOperation("User is not authorized to perform this operation on the order");
        }
    }

    private void validateDeleteOrder(Integer orderId) {
        if(orderId == null || orderId <= 0){
            throw new InvalidDeleteOrderException("Order id cannot be null for the delete");
        }
    }

    private LocalDate formatOrderDate(String date) {
        try {
            if (date == null) return null;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            return LocalDate.parse(date, formatter);

        } catch (Exception e) {
            throw new InvalidOrderDateException("The date format is invalid. Use dd/MM/yyyy");
        }
    }

}
