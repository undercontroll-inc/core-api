package com.undercontroll.application.usecase;

import com.undercontroll.domain.port.in.GetDemandsPort;
import com.undercontroll.domain.entity.Order;
import com.undercontroll.domain.exception.InvalidDemandException;
import com.undercontroll.infrastructure.persistence.repository.DemandRepository;
import com.undercontroll.infrastructure.persistence.repository.OrderJpaRepository;
import com.undercontroll.infrastructure.web.dto.DemandDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetDemandsImpl implements GetDemandsPort {

    private final DemandRepository demandRepository;
    private final OrderJpaRepository orderRepository;

    @Override
    public Output execute(Input input) {
        Order order = orderRepository.findById(input.orderId())
                .orElseThrow(() -> new InvalidDemandException("Order not found with id: " + input.orderId()));

        List<DemandDto> demands = demandRepository.findByOrder(order).stream()
                .map(this::mapToDto)
                .toList();

        return new Output(demands);
    }

    private DemandDto mapToDto(Object demand) {
        return new DemandDto(
                (Integer) ((Object[]) demand)[0],
                (Integer) ((Object[]) demand)[1],
                (Integer) ((Object[]) demand)[2],
                ((Number) ((Object[]) demand)[3]).longValue()
        );
    }
}
