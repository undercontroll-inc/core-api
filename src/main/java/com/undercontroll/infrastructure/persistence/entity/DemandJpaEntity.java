package com.undercontroll.infrastructure.persistence.entity;

import com.undercontroll.domain.model.Demand;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "demand")
public class DemandJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Long quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    private ComponentPartJpaEntity component;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private OrderJpaEntity order;

    public Demand toDomain() {
        return Demand.builder()
                .id(id)
                .quantity(quantity)
                .component(component != null ? component.toDomain() : null)
                .order(order != null ? order.toDomain() : null)
                .build();
    }

    public static DemandJpaEntity fromDomain(Demand demand) {
        if (demand == null) return null;
        return DemandJpaEntity.builder()
                .id(demand.getId())
                .quantity(demand.getQuantity())
                .component(ComponentPartJpaEntity.fromDomain(demand.getComponent()))
                .order(OrderJpaEntity.fromDomain(demand.getOrder()))
                .build();
    }

}
