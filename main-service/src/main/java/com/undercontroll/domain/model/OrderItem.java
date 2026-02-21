package com.undercontroll.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class OrderItem {

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
