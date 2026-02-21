package com.undercontroll.domain.model;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ComponentPart {

    private Integer id;

    private String name;

    private String description;

    private String brand;

    private Double price;

    private String supplier;

    private String category;

    private Long quantity;

    @Builder.Default
    private List<Demand> demands = new ArrayList<>();

}
