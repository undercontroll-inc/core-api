package com.undercontroll.domain.stockimport;

import com.undercontroll.domain.enums.StockImportItemWarning;
import com.undercontroll.domain.enums.StockImportWarning;
import com.undercontroll.domain.model.stockimport.StockImport;
import com.undercontroll.domain.model.stockimport.StockImportItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class StockImportWarnings {

    static final double LOW_CONFIDENCE = 0.6;
    static final BigDecimal ITEM_ABSOLUTE_TOLERANCE = new BigDecimal("0.05");
    static final BigDecimal IMPORT_ABSOLUTE_TOLERANCE = new BigDecimal("0.10");
    static final BigDecimal RELATIVE_TOLERANCE = new BigDecimal("0.01");

    private StockImportWarnings() {
    }

    public static StockImportItem withWarnings(StockImportItem item) {
        List<StockImportItemWarning> warnings = new ArrayList<>();
        if (item.unitPrice() == null) {
            warnings.add(StockImportItemWarning.MISSING_UNIT_PRICE);
        }
        if (item.quantity() != null && item.unitPrice() != null && item.total() != null
                && mismatch(item.quantity().multiply(item.unitPrice()), item.total(), ITEM_ABSOLUTE_TOLERANCE)) {
            warnings.add(StockImportItemWarning.TOTAL_MISMATCH);
        }
        if (item.confidence() < LOW_CONFIDENCE) {
            warnings.add(StockImportItemWarning.LOW_CONFIDENCE);
        }
        if (item.quantity() != null && !isWhole(item.quantity())) {
            warnings.add(StockImportItemWarning.NON_INTEGER_QUANTITY);
        }
        return item.toBuilder().warnings(warnings).build();
    }

    public static List<StockImportWarning> importWarnings(StockImport stockImport) {
        List<StockImportWarning> warnings = new ArrayList<>();
        BigDecimal itemsTotal = stockImport.itemsTotal();
        if (stockImport.total() != null && itemsTotal != null
                && mismatch(itemsTotal, stockImport.total(), IMPORT_ABSOLUTE_TOLERANCE)) {
            warnings.add(StockImportWarning.ITEMS_TOTAL_MISMATCH);
        }
        if (stockImport.duplicateOfImportId() != null) {
            warnings.add(StockImportWarning.POSSIBLE_DUPLICATE);
        }
        return warnings;
    }

    public static boolean isWhole(BigDecimal value) {
        return value.signum() == 0 || value.stripTrailingZeros().scale() <= 0;
    }

    private static boolean mismatch(BigDecimal computed, BigDecimal declared, BigDecimal absoluteTolerance) {
        BigDecimal relative = declared.abs().multiply(RELATIVE_TOLERANCE);
        BigDecimal tolerance = relative.compareTo(absoluteTolerance) > 0 ? relative : absoluteTolerance;
        return computed.subtract(declared).abs().compareTo(tolerance) > 0;
    }
}
