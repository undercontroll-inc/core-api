package com.undercontroll.application.usecase;

import com.undercontroll.domain.port.in.UpdateOrderPort;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.exception.OrderNotFoundException;
import com.undercontroll.domain.exception.InvalidUpdateOrderException;
import com.undercontroll.domain.port.out.OrderRepositoryPort;
import com.undercontroll.domain.port.out.MetricsPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateOrderImpl implements UpdateOrderPort {

    private final OrderRepositoryPort orderRepositoryPort;
    private final MetricsPort metricsPort;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"orders", "ordersByUser", "order", "orderParts", "dashboardMetrics"}, allEntries = true)
    public Output execute(Input input) {
        try {
            log.info("Updating order {}", input.orderId());

            validateUpdateOrder(input.orderId());

            Order order = orderRepositoryPort.findById(input.orderId())
                    .orElseThrow(() -> new OrderNotFoundException("Could not found the order while updating."));

            if (input.status() != null) {
                order.setStatus(input.status());
                log.info("Order {} status updated to {}", input.orderId(), input.status());

                if (input.status().name().equals("COMPLETED")) {
                    metricsPort.incrementOrderCompleted();
                }
            }

            if (input.serviceDescription() != null) {
                order.setDescription(input.serviceDescription());
            }

            orderRepositoryPort.save(order);
            log.info("Order {} updated successfully", input.orderId());

            return new Output(true, "Order updated successfully");
        } catch (Exception e) {
            metricsPort.incrementOrderUpdateFailed();
            throw e;
        }
    }

    private void validateUpdateOrder(Integer id) {
        if (id == null || id <= 0) {
            throw new InvalidUpdateOrderException("Order id cannot be null for the update");
        }
    }
}
