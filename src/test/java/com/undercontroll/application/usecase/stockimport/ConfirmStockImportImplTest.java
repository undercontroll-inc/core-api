package com.undercontroll.application.usecase.stockimport;

import com.undercontroll.domain.enums.StockImportItemAction;
import com.undercontroll.domain.enums.StockImportStatus;
import com.undercontroll.domain.exception.StockImportComponentNotFoundException;
import com.undercontroll.domain.exception.StockImportInvalidStateException;
import com.undercontroll.domain.gateway.ComponentAliasGateway;
import com.undercontroll.domain.gateway.ComponentGateway;
import com.undercontroll.domain.gateway.StockImportGateway;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.model.stockimport.ItemResult;
import com.undercontroll.domain.model.stockimport.NewComponentSpec;
import com.undercontroll.domain.model.stockimport.StockImport;
import com.undercontroll.domain.model.stockimport.StockImportItem;
import com.undercontroll.domain.model.stockimport.StockImportReview;
import com.undercontroll.domain.usecase.stockimport.impl.ConfirmStockImportImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfirmStockImportImplTest {

    private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");

    private StockImportGateway gateway;
    private ComponentGateway components;
    private ComponentAliasGateway aliases;
    private ConfirmStockImportImpl confirm;

    @BeforeEach
    void setUp() {
        gateway = mock(StockImportGateway.class);
        components = mock(ComponentGateway.class);
        aliases = mock(ComponentAliasGateway.class);
        confirm = new ConfirmStockImportImpl(gateway, components, aliases, Clock.fixed(NOW, ZoneOffset.UTC));
        when(gateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static StockImport ready() {
        return StockImport.builder()
                .id(1L)
                .status(StockImportStatus.READY)
                .supplierName("Eletro Pecas")
                .supplierCnpj("12345678000195")
                .items(List.of(
                        StockImportItem.builder().id(10L).lineNumber(1).supplierCode("RES-10K").description("Resistor 10k")
                                .quantity(new BigDecimal("100")).unitPrice(new BigDecimal("0.15")).confidence(1.0)
                                .action(StockImportItemAction.ADD_TO_EXISTING).componentId(5).build(),
                        StockImportItem.builder().id(11L).lineNumber(2).supplierCode("MOT").description("Motor 220V")
                                .quantity(new BigDecimal("2")).unitPrice(new BigDecimal("89.90")).confidence(1.0)
                                .action(StockImportItemAction.CREATE_NEW).build(),
                        StockImportItem.builder().id(12L).lineNumber(3).description("Frete")
                                .quantity(BigDecimal.ONE).confidence(1.0)
                                .action(StockImportItemAction.IGNORE).build()))
                .build();
    }

    private static StockImportReview review(Integer componentId) {
        return new StockImportReview(null, null, null, null, null, List.of(
                new StockImportReview.Item(10L, "Resistor 10k", new BigDecimal("100"), "UN", new BigDecimal("0.15"),
                        StockImportItemAction.ADD_TO_EXISTING, componentId, true, null),
                new StockImportReview.Item(11L, "Motor 220V", new BigDecimal("2.000"), "PC", new BigDecimal("89.90"),
                        StockImportItemAction.CREATE_NEW, null, false,
                        new NewComponentSpec("Motor liquidificador 220V", "Arno", "Motores", null)),
                new StockImportReview.Item(12L, "Frete", BigDecimal.ONE, null, null,
                        StockImportItemAction.IGNORE, null, false, null)));
    }

    @Test
    @DisplayName("adds to existing, creates new, skips ignored, learns aliases and marks CONFIRMED")
    void confirms() {
        when(gateway.findByIdForUpdate(1L)).thenReturn(Optional.of(ready()));
        when(components.existsById(5)).thenReturn(true);
        when(components.addStock(5, 100L, 0.15)).thenReturn(Optional.of(
                ComponentPart.builder().id(5).name("Resistor").quantity(130L).build()));
        when(components.save(any())).thenAnswer(invocation -> {
            ComponentPart part = invocation.getArgument(0);
            part.setId(42);
            return part;
        });

        StockImport confirmed = confirm.execute(1L, review(5));

        assertEquals(StockImportStatus.CONFIRMED, confirmed.status());
        assertEquals(NOW, confirmed.confirmedAt());
        assertEquals(new ItemResult(5, false, 30L, 130L), confirmed.items().get(0).result());
        assertEquals(new ItemResult(42, true, 0L, 2L), confirmed.items().get(1).result());
        assertNull(confirmed.items().get(2).result());

        ArgumentCaptor<ComponentPart> created = ArgumentCaptor.forClass(ComponentPart.class);
        verify(components).save(created.capture());
        assertEquals("Motor liquidificador 220V", created.getValue().getName());
        assertEquals("Motor 220V", created.getValue().getDescription());
        assertEquals("Arno", created.getValue().getBrand());
        assertEquals("Eletro Pecas", created.getValue().getSupplier());
        assertEquals(2L, created.getValue().getQuantity());
        assertEquals(89.90, created.getValue().getPrice());

        verify(aliases).upsert("cnpj:12345678000195", "code:res-10k", 5, NOW);
        verify(aliases).upsert("cnpj:12345678000195", "code:mot", 42, NOW);
    }

    @Test
    @DisplayName("unknown componentId is rejected before touching stock")
    void unknownComponent() {
        when(gateway.findByIdForUpdate(1L)).thenReturn(Optional.of(ready()));
        when(components.existsById(9)).thenReturn(false);

        StockImportComponentNotFoundException ex = assertThrows(StockImportComponentNotFoundException.class,
                () -> confirm.execute(1L, review(9)));

        assertEquals("items[0].componentId", ex.violations().getFirst().field());
        verify(components, never()).addStock(anyInt(), eq(100L), any());
        verify(gateway, never()).save(any());
    }

    @Test
    @DisplayName("a second confirm is a 409, not a second stock increment")
    void alreadyConfirmed() {
        when(gateway.findByIdForUpdate(1L)).thenReturn(Optional.of(ready().toBuilder().status(StockImportStatus.CONFIRMED).build()));

        assertThrows(StockImportInvalidStateException.class, () -> confirm.execute(1L, review(5)));
        verify(components, never()).addStock(any(), eq(100L), any());
    }
}
