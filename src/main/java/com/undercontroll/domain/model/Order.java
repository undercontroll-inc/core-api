package com.undercontroll.domain.model;

import com.undercontroll.domain.enums.OrderStatus;
import lombok.*;

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
    private String description;
    private String nf;
    private Date date;
    private String store;

    private LocalDateTime updatedAt;

    private LocalDate received_at;

    private LocalDate completedTime;

    private LocalDateTime createdAt;

    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
    }

    public void removeOrderItem(OrderItem orderItem) {
        this.orderItems.remove(orderItem);
    }

    public Double calculateLaborTotal() {
        return this.getOrderItems().stream()
                .mapToDouble(OrderItem::getLaborValue)
                .sum();
    }

    public Double calculatePartsTotal(){
        return this.demands
                .stream()
                .mapToDouble(d -> d.getQuantity() + d.getComponent().getPrice())
                .sum();
    }

    public Double calculateTotal() {
        return this.calculatePartsTotal() + this.calculateLaborTotal() - this.discount;
    }

}
