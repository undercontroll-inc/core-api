package com.undercontroll.infrastructure.gateway;

import com.undercontroll.domain.gateway.OrderStatusEventGateway;
import com.undercontroll.domain.model.OrderStatusEvent;
import com.undercontroll.infrastructure.persistence.entity.OrderStatusEventJpaEntity;
import com.undercontroll.infrastructure.persistence.repository.OrderPhotoJpaRepository;
import com.undercontroll.infrastructure.persistence.repository.OrderStatusEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderStatusEventGatewayImpl implements OrderStatusEventGateway {

    private final OrderStatusEventJpaRepository events;

    @Override
    @Transactional
    public OrderStatusEvent save(OrderStatusEvent event) {
        return toDomain(events.save(OrderStatusEventJpaEntity.builder()
                .orderId(event.orderId())
                .fromStatus(event.from())
                .toStatus(event.to())
                .outcome(event.outcome())
                .actor(event.actor())
                .note(event.note())
                .createdAt(event.createdAt())
                .build()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusEvent> findByOrderId(Integer orderId) {
        return events.findByOrderIdOrderByCreatedAtAscIdAsc(orderId).stream().map(OrderStatusEventGatewayImpl::toDomain).toList();
    }

    private static OrderStatusEvent toDomain(OrderStatusEventJpaEntity entity) {
        return new OrderStatusEvent(entity.getId(), entity.getOrderId(), entity.getFromStatus(), entity.getToStatus(),
                entity.getOutcome(), entity.getActor(), entity.getNote(), entity.getCreatedAt());
    }
}
