package com.undercontroll.application.dto.stockimport;

import com.undercontroll.domain.enums.StockImportItemAction;
import com.undercontroll.domain.enums.StockImportItemWarning;
import com.undercontroll.domain.enums.StockImportMatchType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

public record StockImportItemDto(
        Long id,
        int lineNumber,
        String supplierCode,
        String description,
        BigDecimal quantity,
        String unit,
        BigDecimal unitPrice,
        BigDecimal total,
        @Schema(description = "Extraction confidence 0..1") double confidence,
        List<StockImportItemWarning> warnings,
        Match match,
        StockImportItemAction action,
        @Schema(description = "Component chosen for ADD_TO_EXISTING") Integer componentId,
        boolean updatePrice,
        NewComponent newComponent,
        Result result
) {

    public record Match(
            StockImportMatchType type,
            @Schema(description = "Suggested component; null when NONE") Integer componentId,
            double confidence,
            List<Candidate> candidates
    ) {
    }

    public record Candidate(
            Integer componentId,
            String name,
            String brand,
            @Schema(description = "Stock quantity when the invoice was matched") Long currentQuantity,
            Double price,
            double score
    ) {
    }

    public record NewComponent(String name, String brand, String category, String description) {
    }

    public record Result(Integer componentId, boolean created, long previousQuantity, long newQuantity) {
    }
}
