package com.undercontroll.application.usecase;

import com.undercontroll.domain.port.in.UpdateOrderPort;
import com.undercontroll.domain.entity.Order;
import com.undercontroll.domain.exception.OrderNotFoundException;
import com.undercontroll.domain.exception.InvalidUpdateOrderException;
import com.undercontroll.infrastructure.persistence.repository.OrderJpaRepository;
import com.undercontroll.application.service.OrderItemService;
import com.undercontroll.application.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateOrderImpl implements UpdateOrderPort {

    private final OrderJpaRepository repository;
    private final OrderItemService orderItemService;
    private final MetricsService metricsService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"orders", "ordersByUser", "order", "orderParts", "dashboardMetrics"}, allEntries = true)
    public Output execute(Input input) {
        try {
            log.info("Updating order {}", input.orderId());

            validateUpdateOrder(input.orderId());

            Order order = repository.findById(input.orderId())
                    .orElseThrow(() -> new OrderNotFoundException("Could not found the order while updating."));

            if (input.status() != null) {
                order.setStatus(input.status());
                log.info("Order {} status updated to {}", input.orderId(), input.status());

                if (input.status().name().equals("COMPLETED")) {
                    metricsService.incrementOrderCompleted();
                }
            }

            if (input.serviceDescription() != null) {
                order.setDescription(input.serviceDescription());
            }

            repository.save(order);
            log.info("Order {} updated successfully", input.orderId());

            return new Output(true, "Order updated successfully");
        } catch (Exception e) {
            metricsService.incrementOrderUpdateFailed();
            throw e;
        }
    }

    private void validateUpdateOrder(Integer id) {
        if (id == null || id <= 0) {
            throw new InvalidUpdateOrderException("Order id cannot be null for the update");
        }
    }
}
