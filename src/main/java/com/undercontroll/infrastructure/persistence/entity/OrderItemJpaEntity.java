package com.undercontroll.infrastructure.persistence.entity;

import com.undercontroll.domain.model.OrderItem;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
@Entity
@Table(name = "order_item")
public class OrderItemJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String imageUrl;

    private String observation;
    private String volt;
    private String series;
    private String type;
    private String brand;
    private String model;

    private Double laborValue;

    private LocalDateTime completedAt;

    public OrderItem toDomain() {
        return OrderItem.builder()
                .id(id)
                .imageUrl(imageUrl)
                .observation(observation)
                .volt(volt)
                .series(series)
                .type(type)
                .brand(brand)
                .model(model)
                .laborValue(laborValue)
                .completedAt(completedAt)
                .build();
    }

    public static OrderItemJpaEntity fromDomain(OrderItem orderItem) {
        if (orderItem == null) return null;
        return OrderItemJpaEntity.builder()
                .id(orderItem.getId())
                .imageUrl(orderItem.getImageUrl())
                .observation(orderItem.getObservation())
                .volt(orderItem.getVolt())
                .series(orderItem.getSeries())
                .type(orderItem.getType())
                .brand(orderItem.getBrand())
                .model(orderItem.getModel())
                .laborValue(orderItem.getLaborValue())
                .completedAt(orderItem.getCompletedAt())
                .build();
    }

}
