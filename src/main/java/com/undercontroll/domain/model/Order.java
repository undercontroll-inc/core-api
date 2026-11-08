package com.undercontroll.domain.model;

import com.undercontroll.domain.enums.ApprovalDecision;
import com.undercontroll.domain.enums.OrderActor;
import com.undercontroll.domain.enums.OrderOutcome;
import com.undercontroll.domain.enums.OrderStatus;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Order {

    private Integer id;

    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    @Builder.Default
    private List<Demand> demands = new ArrayList<>();

    private User user;

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

    private LocalDateTime updatedAt;

    private LocalDate received_at;

    private LocalDate deadline;

    private OrderOutcome outcome;

    private Instant completedAt;

    private Instant deliveredAt;

    private String trackingToken;

    private Instant budgetSentAt;
    private Double budgetLaborTotal;
    private Double budgetPartsTotal;
    private Double budgetDiscount;
    private Double budgetTotal;

    private ApprovalDecision approvalDecision;
    private Instant decidedAt;
    private OrderActor decidedVia;

    private Long version;

    private LocalDateTime createdAt;

    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
    }

    public void removeOrderItem(OrderItem orderItem) {
        this.orderItems.remove(orderItem);
    }

    public void addDemand(Demand demand) {
        this.demands.add(demand);
    }

    public void removeDemand(Demand demand) {
        this.demands.remove(demand);
    }

    public Double calculateLaborTotal() {
        return this.getOrderItems().stream()
                .mapToDouble(item -> item.getLaborValue() == null ? 0.0 : item.getLaborValue())
                .sum();
    }

    public Double calculatePartsTotal() {
        return this.demands
                .stream()
                .filter(d -> d.getQuantity() != null && d.getComponent() != null && d.getComponent().getPrice() != null)
                .mapToDouble(d -> d.getQuantity() * d.getComponent().getPrice())
                .sum();
    }

    public Double calculateTotal() {
        return this.calculatePartsTotal() + this.calculateLaborTotal() - (this.discount == null ? 0.0 : this.discount);
    }

    public void recalculateTotal() {
        this.total = calculateTotal();
    }

}
