package com.undercontroll.application.dto.stockimport;

import com.undercontroll.domain.enums.StockImportSourceType;
import com.undercontroll.domain.enums.StockImportStage;
import com.undercontroll.domain.enums.StockImportStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;

public record StockImportSummaryDto(
        Long id,
        StockImportStatus status,
        StockImportStage stage,
        StockImportSourceType sourceType,
        String supplierName,
        String invoiceNumber,
        OffsetDateTime issuedAt,
        BigDecimal total,
        int itemCount,
        Instant createdAt
) {
}
