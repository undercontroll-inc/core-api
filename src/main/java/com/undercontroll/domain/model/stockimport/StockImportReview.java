package com.undercontroll.domain.model.stockimport;

import com.undercontroll.domain.enums.StockImportItemAction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record StockImportReview(
        String supplierName,
        String supplierCnpj,
        String invoiceNumber,
        String invoiceSeries,
        OffsetDateTime issuedAt,
        List<Item> items
) {

    public record Item(
            Long id,
            String description,
            BigDecimal quantity,
            String unit,
            BigDecimal unitPrice,
            StockImportItemAction action,
            Integer componentId,
            boolean updatePrice,
            NewComponentSpec newComponent
    ) {
    }
}
