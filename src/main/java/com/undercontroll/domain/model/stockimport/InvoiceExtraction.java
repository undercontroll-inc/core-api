package com.undercontroll.domain.model.stockimport;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record InvoiceExtraction(
        boolean invoice,
        String supplierName,
        String supplierCnpj,
        String number,
        String series,
        OffsetDateTime issuedAt,
        String accessKey,
        BigDecimal total,
        List<ExtractedItem> items
) {

    public InvoiceExtraction {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public record ExtractedItem(
            String supplierCode,
            String description,
            BigDecimal quantity,
            String unit,
            BigDecimal unitPrice,
            BigDecimal total,
            double confidence
    ) {
    }
}
