package com.undercontroll.domain.model.stockimport;

import com.undercontroll.domain.enums.StockImportItemAction;
import com.undercontroll.domain.enums.StockImportItemWarning;
import com.undercontroll.domain.enums.StockImportMatchType;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder(toBuilder = true)
public record StockImportItem(
        Long id,
        int lineNumber,
        String supplierCode,
        String description,
        BigDecimal quantity,
        String unit,
        BigDecimal unitPrice,
        BigDecimal total,
        double confidence,
        List<StockImportItemWarning> warnings,
        StockImportMatchType matchType,
        Integer matchedComponentId,
        double matchConfidence,
        List<MatchCandidate> candidates,
        StockImportItemAction action,
        Integer componentId,
        boolean updatePrice,
        NewComponentSpec newComponent,
        ItemResult result
) {

    public StockImportItem {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
        matchType = matchType == null ? StockImportMatchType.NONE : matchType;
    }
}
