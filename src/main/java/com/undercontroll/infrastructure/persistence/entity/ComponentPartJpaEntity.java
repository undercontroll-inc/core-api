package com.undercontroll.infrastructure.persistence.entity;

import com.undercontroll.domain.model.ComponentPart;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "component")
public class ComponentPartJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    private String description;

    private String brand;

    private Double price;

    private String supplier;

    private String category;

    private Long quantity;

    @OneToMany(mappedBy = "component", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DemandJpaEntity> demands = new ArrayList<>();

    public ComponentPart toDomain() {
        return ComponentPart.builder()
                .id(id)
                .name(name)
                .description(description)
                .brand(brand)
                .price(price)
                .supplier(supplier)
                .category(category)
                .quantity(quantity)
                .demands(demands != null ? demands.stream().map(DemandJpaEntity::toDomain).toList() : new ArrayList<>())
                .build();
    }

    public static ComponentPartJpaEntity fromDomain(ComponentPart component) {
        if (component == null) return null;
        return ComponentPartJpaEntity.builder()
                .id(component.getId())
                .name(component.getName())
                .description(component.getDescription())
                .brand(component.getBrand())
                .price(component.getPrice())
                .supplier(component.getSupplier())
                .category(component.getCategory())
                .quantity(component.getQuantity())
                .demands(component.getDemands() != null ? component.getDemands().stream().map(DemandJpaEntity::fromDomain).toList() : new ArrayList<>())
                .build();
    }

}
