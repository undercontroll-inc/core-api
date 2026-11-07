package com.undercontroll.application.dto.stockimport;

import com.undercontroll.domain.enums.StockImportItemAction;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Reviewed invoice. Send the full item list; items left out are treated as IGNORE")
public record StockImportReviewRequest(
        StockImportDto.Supplier supplier,
        Invoice invoice,
        List<Item> items
) {

    public record Invoice(String number, String series, OffsetDateTime issuedAt) {
    }

    public record Item(
            @Schema(description = "Existing item id; null for an item added by hand") Long id,
            String description,
            @Schema(description = "Greater than 0; whole number to confirm") BigDecimal quantity,
            String unit,
            BigDecimal unitPrice,
            StockImportItemAction action,
            @Schema(description = "Required when action is ADD_TO_EXISTING") Integer componentId,
            @Schema(description = "ADD_TO_EXISTING only: set the component price to unitPrice") Boolean updatePrice,
            @Schema(description = "CREATE_NEW only; null uses the description as name") StockImportItemDto.NewComponent newComponent
    ) {
    }
}
