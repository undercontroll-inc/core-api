package com.undercontroll.application.usecase;

import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.OrderItem;
import com.undercontroll.domain.model.User;
import com.undercontroll.domain.port.in.GetOrdersPort;
import com.undercontroll.domain.port.out.OrderRepositoryPort;
import com.undercontroll.application.dto.OrderEnrichedDto;
import com.undercontroll.application.dto.OrderItemDto;
import com.undercontroll.application.dto.UserDto;
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

    private final OrderRepositoryPort orderRepositoryPort;

    @Override
    @Cacheable(value = "orders")
    public Output execute(Input input) {
        log.info("Fetching all orders");
        List<Order> orders = orderRepositoryPort.findAll();
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

        Double partsTotal = orderRepositoryPort.calculatePartsTotalByOrderId(order.getId());
        Double laborTotal = calculateLaborTotal(order);

        return new OrderEnrichedDto(
                order.getId(),
                toUserDto(order.getUser()),
                toOrderItemDtos(order.getOrderItems()),
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

    private UserDto toUserDto(User user) {
        if (user == null) return null;
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getLastName(),
                user.getAddress(),
                user.getCpf(),
                user.getCEP(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getHasWhatsApp(),
                user.getAlreadyRecurrent(),
                user.getInFirstLogin(),
                user.getUserType()
        );
    }

    private List<OrderItemDto> toOrderItemDtos(List<OrderItem> orderItems) {
        if (orderItems == null) return null;
        return orderItems.stream()
                .map(item -> new OrderItemDto(
                        item.getId(),
                        item.getImageUrl(),
                        item.getModel(),
                        item.getType(),
                        item.getBrand(),
                        item.getObservation(),
                        item.getVolt(),
                        item.getSeries(),
                        item.getLaborValue(),
                        item.getCompletedAt()
                ))
                .toList();
    }

    private Double calculateLaborTotal(Order order) {
        return order.getOrderItems().stream()
                .mapToDouble(OrderItem::getLaborValue)
                .sum();
    }

    private String formatDate(java.time.LocalDate date, DateTimeFormatter formatter) {
        return date == null ? null : date.format(formatter);
    }
}
