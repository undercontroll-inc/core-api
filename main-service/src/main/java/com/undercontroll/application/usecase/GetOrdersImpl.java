package com.undercontroll.application.usecase;

import com.undercontroll.domain.port.in.GetOrdersPort;
import com.undercontroll.domain.entity.Order;
import com.undercontroll.infrastructure.persistence.repository.OrderJpaRepository;
import com.undercontroll.application.service.UserService;
import com.undercontroll.application.service.OrderItemService;
import com.undercontroll.infrastructure.web.dto.OrderEnrichedDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetOrdersImpl implements GetOrdersPort {

    private final OrderJpaRepository repository;
    private final UserService userService;
    private final OrderItemService orderItemService;

    @Override
    @Cacheable(value = "orders")
    public Output execute(Input input) {
        log.info("Fetching all orders");
        List<Order> orders = repository.findAll();
        List<OrderEnrichedDto> enrichedOrders = mapOrdersToEnrichedDtos(orders);
        return new Output(enrichedOrders);
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

        return new OrderEnrichedDto(
                order.getId(),
                userService.mapToDto(order.getUser()),
                mapOrderItems(order),
                null,
                partsTotal,
                laborTotal,
                order.getDiscount(),
                partsTotal + laborTotal - order.getDiscount(),
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
                .mapToDouble(item -> item.getLaborValue())
                .sum();
    }

    private Object mapOrderItems(Order order) {
        return order.getOrderItems().stream()
                .map(orderItemService::mapToDto)
                .toList();
    }

    private String formatDate(java.time.LocalDate date, DateTimeFormatter formatter) {
        return date == null ? null : date.format(formatter);
    }
}
