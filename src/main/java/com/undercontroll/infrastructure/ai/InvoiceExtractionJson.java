package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.undercontroll.domain.model.stockimport.InvoiceExtraction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InvoiceExtractionJson(
        @JsonProperty("is_invoice") Boolean isInvoice,
        @JsonProperty("supplier_name") String supplierName,
        @JsonProperty("supplier_cnpj") String supplierCnpj,
        @JsonProperty("invoice_number") String invoiceNumber,
        @JsonProperty("invoice_series") String invoiceSeries,
        @JsonProperty("issued_at") String issuedAt,
        @JsonProperty("access_key") String accessKey,
        @JsonProperty("total") BigDecimal total,
        @JsonProperty("items") List<Item> items
) {

    static final ZoneOffset BRASILIA = ZoneOffset.ofHours(-3);

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            @JsonProperty("supplier_code") String supplierCode,
            @JsonProperty("description") String description,
            @JsonProperty("quantity") BigDecimal quantity,
            @JsonProperty("unit") String unit,
            @JsonProperty("unit_price") BigDecimal unitPrice,
            @JsonProperty("total") BigDecimal total,
            @JsonProperty("confidence") Double confidence
    ) {
    }

    public InvoiceExtraction toDomain() {
        List<InvoiceExtraction.ExtractedItem> extracted = items == null ? List.of() : items.stream()
                .limit(InvoiceExtractionSchema.MAX_ITEMS)
                .map(item -> new InvoiceExtraction.ExtractedItem(
                        item.supplierCode(),
                        item.description(),
                        item.quantity(),
                        item.unit(),
                        item.unitPrice(),
                        item.total(),
                        item.confidence() == null ? 0.5 : item.confidence()))
                .toList();
        return new InvoiceExtraction(
                Boolean.TRUE.equals(isInvoice),
                supplierName,
                supplierCnpj,
                invoiceNumber,
                invoiceSeries,
                date(issuedAt),
                accessKey,
                total,
                extracted
        );
    }

    static OffsetDateTime date(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.strip();
        try {
            return LocalDate.parse(trimmed.length() > 10 ? trimmed.substring(0, 10) : trimmed)
                    .atStartOfDay()
                    .atOffset(BRASILIA);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
