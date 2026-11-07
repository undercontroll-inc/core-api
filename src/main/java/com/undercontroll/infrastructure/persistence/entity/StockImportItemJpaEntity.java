package com.undercontroll.infrastructure.persistence.entity;

import com.undercontroll.domain.enums.StockImportItemAction;
import com.undercontroll.domain.enums.StockImportMatchType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stock_import_item", indexes = @Index(name = "ix_stock_import_item_import", columnList = "import_id"))
public class StockImportItemJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "import_id", nullable = false)
    private StockImportJpaEntity stockImport;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @Column(name = "supplier_code", length = 60)
    private String supplierCode;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(length = 20)
    private String unit;

    @Column(name = "unit_price", precision = 18, scale = 4)
    private BigDecimal unitPrice;

    @Column(precision = 14, scale = 2)
    private BigDecimal total;

    @Column(nullable = false)
    private double confidence;

    @Column(length = 255)
    private String warnings;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false, length = 20)
    private StockImportMatchType matchType;

    @Column(name = "matched_component_id")
    private Integer matchedComponentId;

    @Column(name = "match_confidence", nullable = false)
    private double matchConfidence;

    @Column(columnDefinition = "TEXT")
    private String candidates;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockImportItemAction action;

    @Column(name = "component_id")
    private Integer componentId;

    @Column(name = "update_price", nullable = false)
    private boolean updatePrice;

    @Column(name = "new_name")
    private String newName;

    @Column(name = "new_brand")
    private String newBrand;

    @Column(name = "new_category")
    private String newCategory;

    @Column(name = "new_description")
    private String newDescription;

    @Column(name = "result_component_id")
    private Integer resultComponentId;

    @Column(name = "result_created")
    private Boolean resultCreated;

    @Column(name = "result_previous_quantity")
    private Long resultPreviousQuantity;

    @Column(name = "result_new_quantity")
    private Long resultNewQuantity;
}
