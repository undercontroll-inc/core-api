package com.undercontroll.infrastructure.mapper;

import com.undercontroll.domain.model.OrderItem;
import com.undercontroll.infrastructure.persistence.entity.OrderItemJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class OrderItemMapper {

    public OrderItem toDomain(OrderItemJpaEntity entity) {
        return OrderItem.builder()
                .id(entity.getId())
                .imageUrl(entity.getImageUrl())
                .observation(entity.getObservation())
                .volt(entity.getVolt())
                .series(entity.getSeries())
                .type(entity.getType())
                .brand(entity.getBrand())
                .model(entity.getModel())
                .laborValue(entity.getLaborValue())
                .completedAt(entity.getCompletedAt())
                .build();
    }

}
