package com.undercontroll.infrastructure.persistence.entity;

import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "`order`")
public class OrderJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    @Builder.Default
    private List<OrderItemJpaEntity> orderItems = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DemandJpaEntity> demands = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    private UserJpaEntity user;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Double total;
    private Double discount;

    private boolean fabricGuarantee;
    private boolean returnGuarantee;
    private String description;
    private String nf;
    private Date date;
    private String store;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDate received_at;

    private LocalDate completedTime;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public Order toDomain() {
        return Order.builder()
                .id(id)
                .orderItems(orderItems != null ? orderItems.stream().map(OrderItemJpaEntity::toDomain).toList() : new ArrayList<>())
                .demands(demands != null ? demands.stream().map(DemandJpaEntity::toDomain).toList() : new ArrayList<>())
                .user(user != null ? user.toDomain() : null)
                .status(status)
                .total(total)
                .discount(discount)
                .fabricGuarantee(fabricGuarantee)
                .returnGuarantee(returnGuarantee)
                .description(description)
                .nf(nf)
                .date(date)
                .store(store)
                .updatedAt(updatedAt)
                .received_at(received_at)
                .completedTime(completedTime)
                .createdAt(createdAt)
                .build();
    }

    public static OrderJpaEntity fromDomain(Order order) {
        if (order == null) return null;
        return OrderJpaEntity.builder()
                .id(order.getId())
                .orderItems(order.getOrderItems() != null ? order.getOrderItems().stream().map(OrderItemJpaEntity::fromDomain).toList() : new ArrayList<>())
                .demands(order.getDemands() != null ? order.getDemands().stream().map(DemandJpaEntity::fromDomain).toList() : new ArrayList<>())
                .user(UserJpaEntity.fromDomain(order.getUser()))
                .status(order.getStatus())
                .total(order.getTotal())
                .discount(order.getDiscount())
                .fabricGuarantee(order.isFabricGuarantee())
                .returnGuarantee(order.isReturnGuarantee())
                .description(order.getDescription())
                .nf(order.getNf())
                .date(order.getDate())
                .store(order.getStore())
                .updatedAt(order.getUpdatedAt())
                .received_at(order.getReceived_at())
                .completedTime(order.getCompletedTime())
                .createdAt(order.getCreatedAt())
                .build();
    }

    public void addOrderItem(OrderItemJpaEntity orderItem) {
        this.orderItems.add(orderItem);
    }

    public void removeOrderItem(OrderItemJpaEntity orderItem) {
        this.orderItems.remove(orderItem);
    }
}
