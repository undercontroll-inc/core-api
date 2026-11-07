package com.undercontroll.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stock_import_file")
public class StockImportFileJpaEntity {

    @Id
    @Column(name = "import_id")
    private Long importId;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false, length = 11_000_000)
    private byte[] content;
}
