package com.undercontroll.application.usecase.demand.impl;

import com.undercontroll.application.usecase.demand.DeleteAllDemandsByOrderPort;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.repository.DemandRepositoryPort;
import com.undercontroll.domain.repository.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeleteAllDemandsByOrderImpl implements DeleteAllDemandsByOrderPort {

    private final DemandRepositoryPort demandRepositoryPort;
    private final OrderRepositoryPort orderRepositoryPort;

    @Override
    public Output execute(Input input) {
        try {
            Optional<Order> orderOpt = orderRepositoryPort.findById(input.orderId());
            if (orderOpt.isEmpty()) {
                return new Output(false, "Order not found");
            }
            Order order = orderOpt.get();
            demandRepositoryPort.deleteByOrder(order);
            return new Output(true, "Demands deleted successfully");
        } catch (Exception e) {
            return new Output(false, "Failed to delete demands: " + e.getMessage());
        }
    }
}
