package com.undercontroll.infrastructure.persistence.entity;

import com.undercontroll.domain.enums.ApprovalDecision;
import com.undercontroll.domain.enums.OrderActor;
import com.undercontroll.domain.enums.OrderOutcome;
import com.undercontroll.domain.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
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
@Table(name = "`orders`")
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
    private String customerDescription;
    private String technicalDescription;
    private String nf;
    private Date date;
    private String store;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDate received_at;

    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OrderOutcome outcome;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "tracking_token", length = 32, unique = true)
    private String trackingToken;

    @Column(name = "budget_sent_at")
    private Instant budgetSentAt;

    @Column(name = "budget_labor_total")
    private Double budgetLaborTotal;

    @Column(name = "budget_parts_total")
    private Double budgetPartsTotal;

    @Column(name = "budget_discount")
    private Double budgetDiscount;

    @Column(name = "budget_total")
    private Double budgetTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_decision", length = 20)
    private ApprovalDecision approvalDecision;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "decided_via", length = 20)
    private OrderActor decidedVia;

    @Version
    private Long version;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public void addOrderItem(OrderItemJpaEntity orderItem) {
        this.orderItems.add(orderItem);
    }

    public void removeOrderItem(OrderItemJpaEntity orderItem) {
        this.orderItems.remove(orderItem);
    }
}
