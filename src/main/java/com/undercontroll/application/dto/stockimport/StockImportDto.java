package com.undercontroll.application.dto.stockimport;

import com.undercontroll.domain.enums.StockImportFailureReason;
import com.undercontroll.domain.enums.StockImportSourceType;
import com.undercontroll.domain.enums.StockImportStage;
import com.undercontroll.domain.enums.StockImportStatus;
import com.undercontroll.domain.enums.StockImportWarning;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Invoice import into stock")
public record StockImportDto(
        Long id,
        StockImportStatus status,
        @Schema(description = "Progress while PROCESSING; null otherwise") StockImportStage stage,
        StockImportSourceType sourceType,
        String fileName,
        StockImportFailureReason failureReason,
        String failureMessage,
        Supplier supplier,
        Invoice invoice,
        @Schema(description = "Sum of item totals, computed by the server") BigDecimal itemsTotal,
        List<StockImportWarning> warnings,
        Long duplicateOfImportId,
        List<StockImportItemDto> items,
        Instant createdAt,
        Instant updatedAt,
        Instant confirmedAt
) {

    public record Supplier(
            String name,
            @Schema(description = "CNPJ or CPF, digits only") String cnpj
    ) {
    }

    public record Invoice(
            String number,
            String series,
            OffsetDateTime issuedAt,
            @Schema(description = "44-digit chave de acesso") String accessKey,
            BigDecimal total
    ) {
    }
}
