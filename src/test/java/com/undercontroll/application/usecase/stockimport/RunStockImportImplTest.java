package com.undercontroll.application.usecase.stockimport;

import com.undercontroll.domain.enums.StockImportFailureReason;
import com.undercontroll.domain.enums.StockImportItemAction;
import com.undercontroll.domain.enums.StockImportItemWarning;
import com.undercontroll.domain.enums.StockImportMatchType;
import com.undercontroll.domain.enums.StockImportSourceType;
import com.undercontroll.domain.enums.StockImportStage;
import com.undercontroll.domain.enums.StockImportStatus;
import com.undercontroll.domain.enums.StockImportWarning;
import com.undercontroll.domain.exception.InvalidNfeXmlException;
import com.undercontroll.domain.exception.InvoiceExtractionException;
import com.undercontroll.domain.gateway.InvoiceExtractionGateway;
import com.undercontroll.domain.gateway.NfeXmlGateway;
import com.undercontroll.domain.gateway.ObjectStorageGateway;
import com.undercontroll.domain.storage.StorageArea;
import com.undercontroll.domain.gateway.StockImportGateway;
import com.undercontroll.domain.model.insight.LlmFailure;
import com.undercontroll.domain.model.stockimport.ComponentMatch;
import com.undercontroll.domain.model.stockimport.InvoiceExtraction;
import com.undercontroll.domain.model.stockimport.StockImport;
import com.undercontroll.domain.stockimport.ComponentMatcher;
import com.undercontroll.domain.support.Sleeper;
import com.undercontroll.domain.usecase.stockimport.impl.RunStockImportImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RunStockImportImplTest {

    private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");
    private static final String OWNER = "worker-1";

    private StockImportGateway gateway;
    private InvoiceExtractionGateway ai;
    private ObjectProvider<InvoiceExtractionGateway> aiProvider;
    private NfeXmlGateway xml;
    private ObjectStorageGateway storage;
    private ComponentMatcher matcher;
    private Sleeper sleeper;
    private RunStockImportImpl run;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        gateway = mock(StockImportGateway.class);
        ai = mock(InvoiceExtractionGateway.class);
        aiProvider = mock(ObjectProvider.class);
        when(aiProvider.getIfAvailable()).thenReturn(ai);
        xml = mock(NfeXmlGateway.class);
        matcher = mock(ComponentMatcher.class);
        sleeper = mock(Sleeper.class);
        storage = mock(ObjectStorageGateway.class);
        run = new RunStockImportImpl(gateway, aiProvider, xml, storage, matcher, sleeper, new Random(1),
                Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofSeconds(90));
        when(gateway.findLegacyContent(1L)).thenReturn(Optional.of(new byte[]{1, 2, 3}));
        when(gateway.findConfirmedDuplicate(any(), any(), any())).thenReturn(Optional.empty());
        when(gateway.complete(any(), eq(OWNER), any())).thenReturn(true);
        when(matcher.match(any(), any(), anyString())).thenReturn(ComponentMatch.none(List.of()));
    }

    private static StockImport leased(StockImportSourceType sourceType) {
        return StockImport.builder()
                .id(1L)
                .status(StockImportStatus.PROCESSING)
                .stage(StockImportStage.RECEIVED)
                .sourceType(sourceType)
                .mimeType(sourceType == StockImportSourceType.NFE_XML ? "application/xml" : "image/jpeg")
                .fileSha256("abc")
                .attemptCount(1)
                .build();
    }

    private static InvoiceExtraction extraction(boolean invoice, List<InvoiceExtraction.ExtractedItem> items) {
        return new InvoiceExtraction(invoice, "Eletro Pecas", "12.345.678/0001-95", "1234", "1", null,
                "3526 1112 3456 7800 0195 5500 1000 0012 3410 0001 2345", new BigDecimal("100.00"), items);
    }

    private static InvoiceExtraction.ExtractedItem item(String description, String quantity, String unitPrice, String total) {
        return new InvoiceExtraction.ExtractedItem("C1", description, quantity == null ? null : new BigDecimal(quantity),
                "UN", unitPrice == null ? null : new BigDecimal(unitPrice), total == null ? null : new BigDecimal(total), 0.95);
    }

    @Test
    @DisplayName("image extraction becomes READY with matched items, warnings and normalized header")
    void imageReady() {
        when(ai.extract(any())).thenReturn(extraction(true, List.of(
                item("Motor 220V", "2", "45.00", "90.00"),
                item("Fio 1,5mm", "2.5", "4.00", "12.00"),
                item("  ", "1", "1", "1"))));
        when(matcher.match(eq("cnpj:12345678000195"), eq("C1"), eq("Motor 220V")))
                .thenReturn(new ComponentMatch(StockImportMatchType.FUZZY, 7, 0.9, List.of()));

        run.execute(leased(StockImportSourceType.IMAGE), OWNER);

        ArgumentCaptor<StockImport> captor = ArgumentCaptor.forClass(StockImport.class);
        verify(gateway).complete(captor.capture(), eq(OWNER), eq(NOW));
        StockImport ready = captor.getValue();
        assertEquals(StockImportStatus.READY, ready.status());
        assertEquals("12345678000195", ready.supplierCnpj());
        assertEquals("35261112345678000195550010000012341000012345", ready.accessKey());
        assertEquals(2, ready.items().size());
        assertEquals(StockImportItemAction.ADD_TO_EXISTING, ready.items().get(0).action());
        assertEquals(7, ready.items().get(0).componentId());
        assertEquals(StockImportItemAction.CREATE_NEW, ready.items().get(1).action());
        assertTrue(ready.items().get(1).warnings().contains(StockImportItemWarning.NON_INTEGER_QUANTITY));
        assertTrue(ready.items().get(1).warnings().contains(StockImportItemWarning.TOTAL_MISMATCH));
        assertTrue(ready.warnings().contains(StockImportWarning.ITEMS_TOTAL_MISMATCH));
        verify(gateway).updateStage(1L, OWNER, StockImportStage.EXTRACTING, NOW);
        verify(gateway).updateStage(1L, OWNER, StockImportStage.MATCHING, NOW);
    }

    @Test
    @DisplayName("XML is parsed without the AI and flags a confirmed duplicate")
    void xmlDuplicate() {
        when(xml.parse(any())).thenReturn(extraction(true, List.of(item("Motor", "1", "100.00", "100.00"))));
        when(gateway.findConfirmedDuplicate(eq(1L), eq("35261112345678000195550010000012341000012345"), eq("abc")))
                .thenReturn(Optional.of(4L));

        run.execute(leased(StockImportSourceType.NFE_XML), OWNER);

        ArgumentCaptor<StockImport> captor = ArgumentCaptor.forClass(StockImport.class);
        verify(gateway).complete(captor.capture(), eq(OWNER), any());
        assertEquals(4L, captor.getValue().duplicateOfImportId());
        assertEquals(List.of(StockImportWarning.POSSIBLE_DUPLICATE), captor.getValue().warnings());
        verify(ai, never()).extract(any());
    }

    @Test
    @DisplayName("invalid XML fails with INVALID_XML")
    void invalidXml() {
        when(xml.parse(any())).thenThrow(new InvalidNfeXmlException("XML is not an NF-e"));

        run.execute(leased(StockImportSourceType.NFE_XML), OWNER);

        verify(gateway).fail(1L, OWNER, StockImportFailureReason.INVALID_XML, "XML is not an NF-e", NOW);
    }

    @Test
    @DisplayName("not an invoice fails with NOT_AN_INVOICE")
    void notInvoice() {
        when(ai.extract(any())).thenReturn(extraction(false, List.of()));

        run.execute(leased(StockImportSourceType.IMAGE), OWNER);

        verify(gateway).fail(eq(1L), eq(OWNER), eq(StockImportFailureReason.NOT_AN_INVOICE), anyString(), eq(NOW));
        verify(gateway, never()).complete(any(), any(), any());
    }

    @Test
    @DisplayName("transient AI errors are retried, then reported as AI_UNAVAILABLE")
    void retriesTransient() throws Exception {
        when(ai.extract(any())).thenThrow(new InvoiceExtractionException(LlmFailure.of(LlmFailure.Kind.TRANSIENT), "503", null));

        run.execute(leased(StockImportSourceType.IMAGE), OWNER);

        verify(ai, times(3)).extract(any());
        verify(sleeper, times(2)).sleep(any());
        verify(gateway).fail(eq(1L), eq(OWNER), eq(StockImportFailureReason.AI_UNAVAILABLE), anyString(), eq(NOW));
    }

    @Test
    @DisplayName("invalid model output is not retried and fails as UNREADABLE")
    void invalidOutput() throws Exception {
        when(ai.extract(any())).thenThrow(new InvoiceExtractionException(LlmFailure.of(LlmFailure.Kind.INVALID_OUTPUT), "bad", null));

        run.execute(leased(StockImportSourceType.IMAGE), OWNER);

        verify(ai, times(1)).extract(any());
        verify(sleeper, never()).sleep(any());
        verify(gateway).fail(eq(1L), eq(OWNER), eq(StockImportFailureReason.UNREADABLE), anyString(), eq(NOW));
    }

    @Test
    @DisplayName("AI disabled fails images with AI_UNAVAILABLE")
    void aiDisabled() {
        when(aiProvider.getIfAvailable()).thenReturn(null);

        run.execute(leased(StockImportSourceType.IMAGE), OWNER);

        verify(gateway).fail(eq(1L), eq(OWNER), eq(StockImportFailureReason.AI_UNAVAILABLE), anyString(), eq(NOW));
    }

    @Test
    @DisplayName("gives up after too many lease attempts")
    void tooManyAttempts() {
        run.execute(leased(StockImportSourceType.IMAGE).toBuilder().attemptCount(4).build(), OWNER);

        verify(gateway).fail(eq(1L), eq(OWNER), eq(StockImportFailureReason.TIMEOUT), anyString(), eq(NOW));
        verify(ai, never()).extract(any());
        verify(gateway, never()).findLegacyContent(isNull());
    }

    @Test
    @DisplayName("imports created from an upload read the file from object storage, not from the database")
    void readsFromStorage() {
        when(storage.read(eq(StorageArea.STOCK_IMPORTS), eq("stock-imports/12/a.xml"), anyLong())).thenReturn(new byte[]{9});
        when(xml.parse(new byte[]{9})).thenReturn(extraction(true, List.of(item("Motor", "1", "100.00", "100.00"))));

        run.execute(leased(StockImportSourceType.NFE_XML).toBuilder().storageKey("stock-imports/12/a.xml").build(), OWNER);

        verify(gateway, never()).findLegacyContent(any());
        verify(gateway).complete(any(), eq(OWNER), any());
    }
}
