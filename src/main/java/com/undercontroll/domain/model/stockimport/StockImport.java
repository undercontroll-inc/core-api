package com.undercontroll.domain.model.stockimport;

import com.undercontroll.domain.enums.StockImportFailureReason;
import com.undercontroll.domain.enums.StockImportSourceType;
import com.undercontroll.domain.enums.StockImportStage;
import com.undercontroll.domain.enums.StockImportStatus;
import com.undercontroll.domain.enums.StockImportWarning;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

@Builder(toBuilder = true)
public record StockImport(
        Long id,
        Integer createdBy,
        StockImportStatus status,
        StockImportStage stage,
        StockImportSourceType sourceType,
        String fileName,
        String mimeType,
        String fileSha256,
        String storageKey,
        StockImportFailureReason failureReason,
        String failureMessage,
        String supplierName,
        String supplierCnpj,
        String invoiceNumber,
        String invoiceSeries,
        OffsetDateTime issuedAt,
        String accessKey,
        BigDecimal total,
        List<StockImportWarning> warnings,
        Long duplicateOfImportId,
        int itemCount,
        int attemptCount,
        List<StockImportItem> items,
        Instant createdAt,
        Instant updatedAt,
        Instant confirmedAt
) {

    public StockImport {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        items = items == null ? List.of() : List.copyOf(items);
    }

    public BigDecimal itemsTotal() {
        BigDecimal sum = null;
        for (StockImportItem item : items) {
            if (item.total() != null) {
                sum = sum == null ? item.total() : sum.add(item.total());
            }
        }
        return sum;
    }
}
