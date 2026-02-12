package com.undercontroll.application.service;

import com.undercontroll.infrastructure.web.dto.CreateOrderItemRequest;
import com.undercontroll.infrastructure.web.dto.OrderItemDto;
import com.undercontroll.infrastructure.web.dto.UpdateOrderItemRequest;
import com.undercontroll.domain.exception.InvalidOrderItemException;
import com.undercontroll.domain.exception.OrderItemNotFoundException;
import com.undercontroll.domain.entity.Order;
import com.undercontroll.domain.entity.OrderItem;
import com.undercontroll.infrastructure.persistence.repository.OrderItemJpaRepository;
import com.undercontroll.infrastructure.persistence.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderItemService {

    private final OrderItemJpaRepository repository;
    private final OrderJpaRepository orderRepository;

    public OrderItem createOrderItem(CreateOrderItemRequest request) {
        validateCreateOrderItemRequest(request);

        OrderItem orderItem = OrderItem.builder()
                .brand(request.brand())
                .model(request.model())
                .type(request.type())
                .imageUrl(request.imageUrl())
                .observation(request.observation())
                .volt(request.volt())
                .series(request.series())
                .laborValue(request.laborValue())
                .build();

        return repository.save(orderItem);
    }

    public void updateOrderItem(UpdateOrderItemRequest data) {
        validateUpdateOrderItem(data);

        Optional<OrderItem> orderItem = repository.findById(data.id());

        if(orderItem.isEmpty()) {
            throw new OrderItemNotFoundException("Could not found the order for update with id: %s".formatted(data.id()));
        }

        OrderItem orderFound = orderItem.get();

        // Faltando aqui validar os campos model, brand e type
        if (data.imageUrl() != null) {
            orderFound.setImageUrl(data.imageUrl());
        }
        if (data.observation() != null) {
            orderFound.setObservation(data.observation());
        }
        if (data.volt() != null) {
            orderFound.setVolt(data.volt());
        }
        if (data.series() != null) {
            orderFound.setSeries(data.series());
        }
        if (data.completedAt() != null) {
            orderFound.setCompletedAt(data.completedAt());
        }
        if(data.labor() != null) {
            orderFound.setLaborValue(data.labor());
        }
        if(data.type() != null) {
            orderFound.setType(data.type());
        }
        if(data.brand() != null) {
            orderFound.setBrand(data.brand());
        }
        if(data.model() != null) {
            orderFound.setModel(data.model());
        }

        repository.save(orderFound);
    }
//
//
//    public List<OrderItemDto> getOrderItemsByOrderId(Integer orderId) {
//        if (orderId == null) {
//            throw new InvalidOrderItemException("Order ID cannot be null");
//        }
//
//        return repository
//                .getOrderItemsByOrderId(orderId)
//                .stream()
//                .map(this::mapToDto)
//                .toList();
//    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteOrderItem(Integer orderItemId) {
        if (orderItemId == null) {
            throw new InvalidOrderItemException("Order item ID cannot be null");
        }

        log.info("Attempting to delete order item {}", orderItemId);

        OrderItem orderItem = repository.findById(orderItemId)
                .orElseThrow(() -> {
                    log.error("Order item {} not found for deletion", orderItemId);
                    return new OrderItemNotFoundException(
                            "Could not find order item with id: " + orderItemId);
                });

        // Busca o Order que contém este OrderItem
        Optional<Order> orderOpt = orderRepository.findOrderByOrderItemId(orderItemId);

        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            log.info("Removing order item {} from order {}", orderItemId, order.getId());

            // Remove o item da coleção do Order
            // O orphanRemoval=true irá deletar o OrderItem automaticamente
            order.getOrderItems().removeIf(item -> item.getId().equals(orderItemId));

            // Salva o Order - o JPA vai cuidar da deleção do órfão
            orderRepository.save(order);

            log.info("Order item {} removed successfully from order {}", orderItemId, order.getId());
        } else {
            // OrderItem órfão (sem Order associado), pode deletar diretamente
            log.info("Order item {} is orphan, deleting directly", orderItemId);
            repository.delete(orderItem);
        }

        log.info("Order item {} deleted successfully", orderItemId);
    }

    public OrderItemDto getOrderItemById(Integer orderItemId) {
        if (orderItemId == null) {
            throw new InvalidOrderItemException("Order item ID cannot be null");
        }

        OrderItem orderItem = repository.findById(orderItemId).orElseThrow(
                () -> new OrderItemNotFoundException("Could not found the order item with id: %s".formatted(orderItemId))
        );
        return mapToDto(orderItem);
    }

    public List<OrderItemDto> getOrderItems() {
        return repository
                .findAll()
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private void validateCreateOrderItemRequest(CreateOrderItemRequest request) {
        if (request.laborValue() != null && request.laborValue() < 0) {
            throw new InvalidOrderItemException("Order item labor cannot be negative");
        }
    }

    private void validateUpdateOrderItem(UpdateOrderItemRequest orderItem) {
        if (orderItem.id() == null) {
            throw new InvalidOrderItemException("Order item ID cannot be null for update");
        }

        if (orderItem.labor() != null && orderItem.labor() < 0) {
            throw new InvalidOrderItemException("Order item labor cannot be negative");
        }
    }

    public OrderItemDto mapToDto(OrderItem orderItem) {
        return new OrderItemDto(
                orderItem.getId(),
                orderItem.getImageUrl(),
                orderItem.getModel(),
                orderItem.getType(),
                orderItem.getBrand(),
                orderItem.getObservation(),
                orderItem.getVolt(),
                orderItem.getSeries(),
                orderItem.getLaborValue(),
                orderItem.getCompletedAt()
        );
    }
}