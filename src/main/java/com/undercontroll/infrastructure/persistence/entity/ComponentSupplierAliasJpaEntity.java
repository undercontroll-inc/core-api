package com.undercontroll.infrastructure.persistence.entity;

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
@Table(
        name = "component_supplier_alias",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_component_supplier_alias",
                columnNames = {"supplier_key", "product_key"}
        )
)
public class ComponentSupplierAliasJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_key", nullable = false)
    private String supplierKey;

    @Column(name = "product_key", nullable = false)
    private String productKey;

    @Column(name = "component_id", nullable = false)
    private Integer componentId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
