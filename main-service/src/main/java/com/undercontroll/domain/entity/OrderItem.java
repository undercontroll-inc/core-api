package com.undercontroll.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
@Table(name = "order_item")
public class OrderItem {

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


}
