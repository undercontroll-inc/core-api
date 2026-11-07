package com.undercontroll.domain.stockimport;

import com.undercontroll.domain.enums.StockImportItemAction;
import com.undercontroll.domain.enums.StockImportItemWarning;
import com.undercontroll.domain.enums.StockImportStatus;
import com.undercontroll.domain.exception.FieldViolation;
import com.undercontroll.domain.exception.StockImportValidationException;
import com.undercontroll.domain.model.stockimport.NewComponentSpec;
import com.undercontroll.domain.model.stockimport.StockImport;
import com.undercontroll.domain.model.stockimport.StockImportItem;
import com.undercontroll.domain.model.stockimport.StockImportReview;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockImportReviewsTest {

    static StockImport current() {
        return StockImport.builder()
                .id(1L)
                .status(StockImportStatus.READY)
                .supplierName("Eletro Pecas")
                .total(new BigDecimal("30.00"))
                .items(List.of(
                        StockImportItem.builder().id(10L).lineNumber(1).description("Resistor 10k")
                                .quantity(new BigDecimal("100")).unitPrice(new BigDecimal("0.15"))
                                .total(new BigDecimal("15.00")).confidence(0.9)
                                .action(StockImportItemAction.ADD_TO_EXISTING).componentId(5).build(),
                        StockImportItem.builder().id(11L).lineNumber(2).description("Fusivel 5A")
                                .quantity(new BigDecimal("10")).unitPrice(new BigDecimal("1.50"))
                                .total(new BigDecimal("15.00")).confidence(0.9)
                                .action(StockImportItemAction.CREATE_NEW).build()))
                .build();
    }

    static StockImportReview.Item item(Long id, String description, String quantity, StockImportItemAction action, Integer componentId) {
        return new StockImportReview.Item(id, description, new BigDecimal(quantity), "UN", new BigDecimal("1.00"),
                action, componentId, false, null);
    }

    @Test
    @DisplayName("applies edits, ignores omitted items and appends hand-added items in request order")
    void appliesReview() {
        StockImportReview review = new StockImportReview(null, null, "99", null, null, List.of(
                item(10L, "Resistor 10k Ohm", "50", StockImportItemAction.ADD_TO_EXISTING, 6),
                item(null, "Capacitor 10uF", "3", StockImportItemAction.CREATE_NEW, null),
                item(null, "Diodo", "4", StockImportItemAction.CREATE_NEW, null)));

        StockImport updated = StockImportReviews.apply(current(), review, false);

        assertEquals(4, updated.items().size());
        StockImportItem edited = updated.items().get(0);
        assertEquals("Resistor 10k Ohm", edited.description());
        assertEquals(6, edited.componentId());
        assertEquals(0, new BigDecimal("50.00").compareTo(edited.total()));
        assertEquals(StockImportItemAction.IGNORE, updated.items().get(1).action());
        assertEquals("Capacitor 10uF", updated.items().get(2).description());
        assertEquals(3, updated.items().get(2).lineNumber());
        assertEquals("Diodo", updated.items().get(3).description());
        assertNull(updated.items().get(2).id());
        assertEquals("Eletro Pecas", updated.supplierName());
        assertEquals("99", updated.invoiceNumber());
    }

    @Test
    @DisplayName("collects field errors with request indexes")
    void fieldErrors() {
        StockImportReview review = new StockImportReview(null, null, null, null, null, List.of(
                item(10L, " ", "0", StockImportItemAction.ADD_TO_EXISTING, null),
                item(99L, "x", "1", StockImportItemAction.CREATE_NEW, null)));

        StockImportValidationException ex = assertThrows(StockImportValidationException.class,
                () -> StockImportReviews.apply(current(), review, false));

        List<String> fields = ex.violations().stream().map(FieldViolation::field).toList();
        assertTrue(fields.contains("items[0].description"));
        assertTrue(fields.contains("items[0].quantity"));
        assertTrue(fields.contains("items[0].componentId"));
        assertTrue(fields.contains("items[1].id"));
    }

    @Test
    @DisplayName("confirm requires whole quantities except for ignored items")
    void wholeQuantityOnConfirm() {
        StockImportReview review = new StockImportReview(null, null, null, null, null, List.of(
                item(10L, "Fio", "2.5", StockImportItemAction.CREATE_NEW, null),
                item(11L, "Fusivel", "1.5", StockImportItemAction.IGNORE, null)));

        StockImport draft = StockImportReviews.apply(current(), review, false);
        assertTrue(draft.items().getFirst().warnings().contains(StockImportItemWarning.NON_INTEGER_QUANTITY));

        StockImportValidationException ex = assertThrows(StockImportValidationException.class,
                () -> StockImportReviews.apply(current(), review, true));
        assertEquals(List.of("items[0].quantity"), ex.violations().stream().map(FieldViolation::field).toList());
    }

    @Test
    @DisplayName("confirm needs at least one item that is not ignored")
    void nothingToImport() {
        StockImportReview review = new StockImportReview(null, null, null, null, null, List.of(
                item(10L, "Resistor", "1", StockImportItemAction.IGNORE, null)));

        StockImportValidationException ex = assertThrows(StockImportValidationException.class,
                () -> StockImportReviews.apply(current(), review, true));
        assertEquals("items", ex.violations().getFirst().field());
    }

    @Test
    @DisplayName("keeps newComponent only for CREATE_NEW and updatePrice only for ADD_TO_EXISTING")
    void actionSpecificFields() {
        StockImportReview review = new StockImportReview(null, null, null, null, null, List.of(
                new StockImportReview.Item(10L, "Resistor", BigDecimal.ONE, null, null,
                        StockImportItemAction.CREATE_NEW, 5, true, new NewComponentSpec("Resistor 10k", " ", null, null))));

        StockImportItem item = StockImportReviews.apply(current(), review, false).items().getFirst();

        assertNull(item.componentId());
        assertFalse(item.updatePrice());
        assertEquals("Resistor 10k", item.newComponent().name());
        assertNull(item.newComponent().brand());
        assertTrue(item.warnings().contains(StockImportItemWarning.MISSING_UNIT_PRICE));
    }
}
