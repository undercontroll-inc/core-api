package com.undercontroll.domain.stockimport;

import com.undercontroll.domain.enums.StockImportItemAction;
import com.undercontroll.domain.enums.StockImportMatchType;
import com.undercontroll.domain.exception.FieldViolation;
import com.undercontroll.domain.exception.StockImportValidationException;
import com.undercontroll.domain.model.stockimport.NewComponentSpec;
import com.undercontroll.domain.model.stockimport.StockImport;
import com.undercontroll.domain.model.stockimport.StockImportItem;
import com.undercontroll.domain.model.stockimport.StockImportReview;
import com.undercontroll.domain.support.Texts;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class StockImportReviews {

    static final int MAX_TEXT = 255;
    static final int MAX_UNIT = 20;
    static final int MAX_ITEMS = 300;
    static final BigDecimal MAX_QUANTITY = new BigDecimal("1000000");

    private StockImportReviews() {
    }

    public static StockImport apply(StockImport current, StockImportReview review, boolean confirming) {
        List<FieldViolation> violations = new ArrayList<>();
        if (review == null || review.items() == null) {
            throw new StockImportValidationException(List.of(new FieldViolation("items", "items is required")));
        }
        if (review.items().size() > MAX_ITEMS) {
            violations.add(new FieldViolation("items", "at most " + MAX_ITEMS + " items"));
        }
        tooLong(violations, "supplier.name", review.supplierName(), MAX_TEXT);
        tooLong(violations, "invoice.number", review.invoiceNumber(), 60);
        tooLong(violations, "invoice.series", review.invoiceSeries(), 20);

        Map<Long, StockImportItem> existing = current.items().stream()
                .collect(Collectors.toMap(StockImportItem::id, Function.identity()));
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < review.items().size(); i++) {
            validate(violations, i, review.items().get(i), existing, seen, confirming);
        }
        if (confirming && violations.isEmpty() && review.items().stream()
                .allMatch(item -> item.action() == StockImportItemAction.IGNORE)) {
            violations.add(new FieldViolation("items", "at least one item must be added to stock"));
        }
        if (!violations.isEmpty()) {
            throw new StockImportValidationException(violations);
        }

        int nextLine = current.items().stream().mapToInt(StockImportItem::lineNumber).max().orElse(0) + 1;
        List<StockImportItem> items = new ArrayList<>();
        for (StockImportReview.Item reviewed : review.items()) {
            StockImportItem base = reviewed.id() == null
                    ? StockImportItem.builder()
                    .lineNumber(nextLine++)
                    .confidence(1.0)
                    .matchType(StockImportMatchType.NONE)
                    .build()
                    : existing.get(reviewed.id());
            items.add(applyItem(base, reviewed));
        }
        current.items().stream()
                .filter(item -> !seen.contains(item.id()))
                .map(item -> item.toBuilder()
                        .action(StockImportItemAction.IGNORE)
                        .componentId(null)
                        .updatePrice(false)
                        .newComponent(null)
                        .build())
                .forEach(items::add);
        items.sort(Comparator.comparingInt(StockImportItem::lineNumber));

        StockImport updated = current.toBuilder()
                .supplierName(keep(review.supplierName(), current.supplierName()))
                .supplierCnpj(review.supplierCnpj() == null ? current.supplierCnpj() : StockImportKeys.digits(review.supplierCnpj()))
                .invoiceNumber(keep(review.invoiceNumber(), current.invoiceNumber()))
                .invoiceSeries(keep(review.invoiceSeries(), current.invoiceSeries()))
                .issuedAt(review.issuedAt() == null ? current.issuedAt() : review.issuedAt())
                .items(items)
                .itemCount(items.size())
                .build();
        return updated.toBuilder().warnings(StockImportWarnings.importWarnings(updated)).build();
    }

    private static void validate(
            List<FieldViolation> violations,
            int index,
            StockImportReview.Item item,
            Map<Long, StockImportItem> existing,
            Set<Long> seen,
            boolean confirming
    ) {
        String prefix = "items[" + index + "].";
        if (item == null) {
            violations.add(new FieldViolation("items[" + index + "]", "item is required"));
            return;
        }
        if (item.id() != null) {
            if (!existing.containsKey(item.id())) {
                violations.add(new FieldViolation(prefix + "id", "item " + item.id() + " does not belong to this import"));
            } else if (!seen.add(item.id())) {
                violations.add(new FieldViolation(prefix + "id", "item " + item.id() + " is repeated"));
            }
        }
        if (item.description() == null || item.description().isBlank()) {
            violations.add(new FieldViolation(prefix + "description", "description is required"));
        }
        tooLong(violations, prefix + "description", item.description(), MAX_TEXT);
        tooLong(violations, prefix + "unit", item.unit(), MAX_UNIT);
        if (item.action() == null) {
            violations.add(new FieldViolation(prefix + "action", "action is required"));
        }
        boolean ignored = item.action() == StockImportItemAction.IGNORE;
        if (item.quantity() == null || item.quantity().signum() <= 0) {
            violations.add(new FieldViolation(prefix + "quantity", "quantity must be greater than 0"));
        } else if (item.quantity().compareTo(MAX_QUANTITY) > 0) {
            violations.add(new FieldViolation(prefix + "quantity", "quantity must be at most " + MAX_QUANTITY));
        } else if (confirming && !ignored && !StockImportWarnings.isWhole(item.quantity())) {
            violations.add(new FieldViolation(prefix + "quantity", "quantity must be a whole number"));
        }
        if (item.unitPrice() != null && item.unitPrice().signum() < 0) {
            violations.add(new FieldViolation(prefix + "unitPrice", "unitPrice must be at least 0"));
        }
        if (item.action() == StockImportItemAction.ADD_TO_EXISTING && item.componentId() == null) {
            violations.add(new FieldViolation(prefix + "componentId", "componentId is required for ADD_TO_EXISTING"));
        }
        NewComponentSpec spec = item.newComponent();
        if (item.action() == StockImportItemAction.CREATE_NEW && spec != null) {
            tooLong(violations, prefix + "newComponent.name", spec.name(), MAX_TEXT);
            tooLong(violations, prefix + "newComponent.brand", spec.brand(), MAX_TEXT);
            tooLong(violations, prefix + "newComponent.category", spec.category(), MAX_TEXT);
            tooLong(violations, prefix + "newComponent.description", spec.description(), MAX_TEXT);
        }
    }

    private static StockImportItem applyItem(StockImportItem base, StockImportReview.Item reviewed) {
        boolean pricingChanged = !sameNumber(base.quantity(), reviewed.quantity())
                || !sameNumber(base.unitPrice(), reviewed.unitPrice());
        BigDecimal total = base.total();
        if (pricingChanged || total == null) {
            total = reviewed.unitPrice() == null
                    ? null
                    : reviewed.quantity().multiply(reviewed.unitPrice()).setScale(2, RoundingMode.HALF_UP);
        }
        StockImportItemAction action = reviewed.action();
        return StockImportWarnings.withWarnings(base.toBuilder()
                .description(reviewed.description().strip())
                .quantity(reviewed.quantity())
                .unit(Texts.blankToNull(reviewed.unit()))
                .unitPrice(reviewed.unitPrice())
                .total(total)
                .action(action)
                .componentId(action == StockImportItemAction.ADD_TO_EXISTING ? reviewed.componentId() : null)
                .updatePrice(action == StockImportItemAction.ADD_TO_EXISTING && reviewed.updatePrice())
                .newComponent(action == StockImportItemAction.CREATE_NEW ? clean(reviewed.newComponent()) : null)
                .build());
    }

    private static NewComponentSpec clean(NewComponentSpec spec) {
        if (spec == null) {
            return null;
        }
        return new NewComponentSpec(Texts.blankToNull(spec.name()), Texts.blankToNull(spec.brand()),
                Texts.blankToNull(spec.category()), Texts.blankToNull(spec.description()));
    }

    private static boolean sameNumber(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return Objects.equals(a, b);
        }
        return a.compareTo(b) == 0;
    }

    private static String keep(String reviewed, String current) {
        return reviewed == null ? current : Texts.blankToNull(reviewed);
    }

    private static void tooLong(List<FieldViolation> violations, String field, String value, int max) {
        if (value != null && value.strip().length() > max) {
            violations.add(new FieldViolation(field, field.substring(field.lastIndexOf('.') + 1)
                    + " must be at most " + max + " characters"));
        }
    }
}
