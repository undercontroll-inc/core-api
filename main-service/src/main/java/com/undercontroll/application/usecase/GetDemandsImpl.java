package com.undercontroll.application.usecase;

import com.undercontroll.domain.port.in.GetDemandsPort;
import com.undercontroll.domain.model.Demand;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.exception.InvalidDemandException;
import com.undercontroll.domain.port.out.DemandRepositoryPort;
import com.undercontroll.domain.port.out.OrderRepositoryPort;
import com.undercontroll.infrastructure.web.dto.DemandDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetDemandsImpl implements GetDemandsPort {

    private final DemandRepositoryPort demandRepositoryPort;
    private final OrderRepositoryPort orderRepositoryPort;

    @Override
    public Output execute(Input input) {
        Order order = orderRepositoryPort.findById(input.orderId())
                .orElseThrow(() -> new InvalidDemandException("Order not found with id: " + input.orderId()));

        List<DemandDto> demands = demandRepositoryPort.findByOrder(order).stream()
                .map(this::mapToDto)
                .toList();

        return new Output(demands);
    }

    private DemandDto mapToDto(Demand demand) {
        return new DemandDto(
                demand.getId(),
                demand.getComponent().getId(),
                demand.getOrder().getId(),
                demand.getQuantity()
        );
    }
}
