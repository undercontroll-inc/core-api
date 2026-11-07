package com.undercontroll.infrastructure.persistence.entity;

import com.undercontroll.domain.enums.StockImportFailureReason;
import com.undercontroll.domain.enums.StockImportSourceType;
import com.undercontroll.domain.enums.StockImportStage;
import com.undercontroll.domain.enums.StockImportStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "stock_import",
        indexes = {
                @Index(name = "ix_stock_import_runnable", columnList = "status, next_run_at"),
                @Index(name = "ix_stock_import_created", columnList = "created_at"),
                @Index(name = "ix_stock_import_access_key", columnList = "access_key")
        }
)
public class StockImportJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_by")
    private Integer createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockImportStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StockImportStage stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private StockImportSourceType sourceType;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "mime_type", nullable = false, length = 50)
    private String mimeType;

    @Column(name = "file_sha256", nullable = false, length = 64)
    private String fileSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason", length = 30)
    private StockImportFailureReason failureReason;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @Column(name = "supplier_name")
    private String supplierName;

    @Column(name = "supplier_cnpj", length = 20)
    private String supplierCnpj;

    @Column(name = "invoice_number", length = 60)
    private String invoiceNumber;

    @Column(name = "invoice_series", length = 20)
    private String invoiceSeries;

    @Column(name = "issued_at")
    private OffsetDateTime issuedAt;

    @Column(name = "access_key", length = 44)
    private String accessKey;

    @Column(precision = 14, scale = 2)
    private BigDecimal total;

    @Column(length = 255)
    private String warnings;

    @Column(name = "duplicate_of_import_id")
    private Long duplicateOfImportId;

    @Column(name = "item_count", nullable = false)
    private int itemCount;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "lease_owner", length = 100)
    private String leaseOwner;

    @Column(name = "lease_until")
    private Instant leaseUntil;

    @Column(name = "next_run_at")
    private Instant nextRunAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @OneToMany(mappedBy = "stockImport", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber ASC")
    @Builder.Default
    private List<StockImportItemJpaEntity> items = new ArrayList<>();
}
