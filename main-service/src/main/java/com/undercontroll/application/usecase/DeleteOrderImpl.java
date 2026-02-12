package com.undercontroll.application.usecase;

import com.undercontroll.domain.port.in.DeleteOrderPort;
import com.undercontroll.domain.entity.Order;
import com.undercontroll.domain.exception.OrderNotFoundException;
import com.undercontroll.domain.exception.InvalidDeleteOrderException;
import com.undercontroll.infrastructure.persistence.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteOrderImpl implements DeleteOrderPort {

    private final OrderJpaRepository repository;

    @Override
    @CacheEvict(value = {"orders", "ordersByUser", "order", "orderParts", "dashboardMetrics"}, allEntries = true)
    public Output execute(Input input) {
        log.info("Deleting order with id {}", input.orderId());
        validateDeleteOrder(input.orderId());

        Order order = repository.findById(input.orderId())
                .orElseThrow(() -> new OrderNotFoundException("Could not found the order"));

        repository.delete(order);
        log.info("Order {} deleted successfully", input.orderId());

        return new Output(true, "Order deleted successfully");
    }

    private void validateDeleteOrder(Integer orderId) {
        if (orderId == null || orderId <= 0) {
            throw new InvalidDeleteOrderException("Order ID cannot be null or less than or equal to 0");
        }
    }
}
