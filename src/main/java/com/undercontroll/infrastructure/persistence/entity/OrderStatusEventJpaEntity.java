package com.undercontroll.infrastructure.persistence.entity;

import com.undercontroll.domain.enums.OrderActor;
import com.undercontroll.domain.enums.OrderOutcome;
import com.undercontroll.domain.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "order_status_event", indexes = @Index(name = "ix_order_status_event_order", columnList = "order_id, created_at"))
public class OrderStatusEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Integer orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private OrderStatus toStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OrderOutcome outcome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderActor actor;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
